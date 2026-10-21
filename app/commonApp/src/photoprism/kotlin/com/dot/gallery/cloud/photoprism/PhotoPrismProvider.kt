/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism

import android.content.Context
import androidx.core.net.toUri
import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudAuthToken
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.Disconnectable
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.core.capabilities.SyncDelta
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import com.dot.gallery.cloud.sync.PhotoPrismTrashClient
import com.dot.gallery.cloud.sync.PhotoPrismUploadTarget
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.network.LanBindingSocketFactory
import com.dot.gallery.cloud.photoprism.data.api.PhotoPrismApiService
import com.dot.gallery.cloud.photoprism.data.api.PhotoPrismAuthInterceptor
import com.dot.gallery.cloud.photoprism.data.api.PhotoPrismAuthMode
import com.dot.gallery.cloud.photoprism.data.api.PhotoPrismAuthenticator
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismPhotoDto
import com.dot.gallery.core.Resource
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.uri.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class PhotoPrismProvider @Inject constructor(
    private val context: Context,
    private val authInterceptor: PhotoPrismAuthInterceptor,
    private val cloudMediaDao: CloudMediaDao
) : RemoteMediaProvider, SyncCapableProvider, Disconnectable {

    override val providerType = ProviderType.PHOTOPRISM
    override val displayName = "PhotoPrism"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentConfig: CloudServerConfig? = null
    private var baseUrl: String = ""
    private var apiService: PhotoPrismApiService? = null
    private val hashByRemoteId = ConcurrentHashMap<String, String>()

    private val lanSocketFactory = LanBindingSocketFactory(context)

    override val isAvailable: Boolean
        get() = currentConfig != null && _connectionState.value == ConnectionState.CONNECTED

    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.REMOTE_ASSETS,
        ProviderCapability.REMOTE_ALBUMS,
        ProviderCapability.FAVORITE,
        ProviderCapability.TEXT_SEARCH,
        ProviderCapability.SYNC,
        ProviderCapability.TRASH,
        ProviderCapability.ARCHIVE
    )

    private val uploadTarget = PhotoPrismUploadTarget(
        context = context,
        cloudMediaDao = cloudMediaDao,
        api = { requireApi() },
        configId = { currentConfig?.id ?: 0L }
    )

    private val trashClient = PhotoPrismTrashClient(
        cloudMediaDao = cloudMediaDao,
        api = { requireApi() },
        configId = { currentConfig?.id ?: 0L },
        captureTokens = { captureTokensFromHeaders(it) },
        mapPhotos = { mapPhotos(it) }
    )

    override fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        currentConfig = null
        apiService = null
        authInterceptor.clear()
        hashByRemoteId.clear()
        baseUrl = ""
        uploadTarget.clear()
    }

    override fun configure(config: CloudServerConfig) {
        currentConfig = config
        baseUrl = config.serverUrl.trimEnd('/')
        authInterceptor.baseUrl = baseUrl
        authInterceptor.username = config.username
        authInterceptor.password = config.password
        apiService = createApiService(baseUrl)
        printDebug("PhotoPrismProvider: Configured with server ${config.serverUrl}")
    }

    private fun createApiService(serverUrl: String): PhotoPrismApiService {
        val url = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(PhotoPrismAuthenticator(authInterceptor))
            .addInterceptor(logging)
            .socketFactory(lanSocketFactory)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotoPrismApiService::class.java)
    }

    private fun requireApi(): PhotoPrismApiService = apiService
        ?: throw IllegalStateException("PhotoPrismProvider not configured. Call configure() first.")

    private fun createIsolatedApiService(
        serverUrl: String,
        token: String? = null
    ): PhotoPrismApiService {
        val url = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val tempInterceptor = PhotoPrismAuthInterceptor().apply {
            accessToken = token
            baseUrl = serverUrl.trimEnd('/')
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(tempInterceptor)
            .socketFactory(lanSocketFactory)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotoPrismApiService::class.java)
    }

    private fun applyConfigTokens(preview: String?, download: String?) {
        if (!preview.isNullOrBlank()) authInterceptor.previewToken = preview
        if (!download.isNullOrBlank()) authInterceptor.downloadToken = download
    }

    private fun rememberHashes(entities: List<CloudMediaEntity>) {
        for (entity in entities) {
            if (entity.fileId.isNotBlank()) {
                hashByRemoteId[entity.remoteId] = entity.fileId
            }
        }
    }

    private fun mapPhotos(photos: List<PhotoPrismPhotoDto>): List<CloudMediaEntity> {
        val configId = currentConfig?.id ?: 0L
        val entities = photos.map {
            it.toCloudMediaEntity(
                serverConfigId = configId,
                baseUrl = baseUrl,
                previewToken = authInterceptor.previewToken,
                downloadToken = authInterceptor.downloadToken
            )
        }
        rememberHashes(entities)
        return entities
    }

    private fun captureTokensFromHeaders(headers: okhttp3.Headers) {
        headers["X-Preview-Token"]?.takeIf { it.isNotBlank() }?.let {
            authInterceptor.previewToken = it
        }
        headers["X-Download-Token"]?.takeIf { it.isNotBlank() }?.let {
            authInterceptor.downloadToken = it
        }
    }

    // === Auth ===

    override suspend fun testConnection(config: CloudServerConfig): Result<CloudServerInfo> {
        return try {
            val tempUrl = config.serverUrl.trimEnd('/')
            val auth = obtainAccess(tempUrl, config.apiKey, config.username, config.password)
                .getOrElse { return Result.failure(it) }
            val tempApi = createIsolatedApiService(tempUrl, auth.accessToken)
            val configResp = tempApi.getConfig()
            if (configResp.isSuccessful) {
                val body = configResp.body()
                Result.success(
                    CloudServerInfo(
                        version = body?.version.orEmpty().ifBlank { "PhotoPrism" },
                        serverName = body?.name?.takeIf { it.isNotBlank() }
                            ?: "PhotoPrism ${body?.version.orEmpty()}".trim()
                    )
                )
            } else {
                Result.failure(Exception("Connection failed: ${configResp.code()} ${configResp.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun authenticate(config: CloudServerConfig): Result<CloudAuthToken> {
        return try {
            val auth = obtainAccess(baseUrl, config.apiKey, config.username, config.password)
                .getOrElse {
                    _connectionState.value = ConnectionState.ERROR
                    return Result.failure(it)
                }
            authInterceptor.accessToken = auth.accessToken
            authInterceptor.authMode = auth.mode
            authInterceptor.username = config.username
            authInterceptor.password = config.password
            applyConfigTokens(auth.previewToken, auth.downloadToken)
            uploadTarget.userUid = auth.userId?.takeIf { it.isNotBlank() }

            // Ensure preview/download tokens exist (static token path may only get them from /config).
            if (authInterceptor.previewToken.isNullOrBlank() || authInterceptor.downloadToken.isNullOrBlank()) {
                val cfg = requireApi().getConfig()
                if (cfg.isSuccessful) {
                    applyConfigTokens(cfg.body()?.previewToken, cfg.body()?.downloadToken)
                }
            }

            // Upload API needs the user UID; static app passwords often omit it on /config.
            if (uploadTarget.userUid.isNullOrBlank()) {
                val session = requireApi().getSession()
                if (session.isSuccessful) {
                    uploadTarget.userUid = session.body()?.user?.uid?.takeIf { it.isNotBlank() }
                }
            }

            // Warm hash cache from Room so originals/thumbs work after process restart
            // before the next full asset prefetch completes.
            val configId = config.id
            cloudMediaDao.getAllCachedAsync()
                .asSequence()
                .filter {
                    it.providerType == ProviderType.PHOTOPRISM &&
                        it.serverConfigId == configId &&
                        it.fileId.isNotBlank()
                }
                .forEach { hashByRemoteId[it.remoteId] = it.fileId }

            _connectionState.value = ConnectionState.CONNECTED
            Result.success(
                CloudAuthToken(
                    accessToken = auth.accessToken,
                    userId = uploadTarget.userUid ?: auth.userId,
                    userEmail = auth.userEmail,
                    isAdmin = auth.isAdmin
                )
            )
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    private data class ObtainedAuth(
        val accessToken: String,
        val mode: PhotoPrismAuthMode,
        val previewToken: String? = null,
        val downloadToken: String? = null,
        val userId: String? = null,
        val userEmail: String? = null,
        val isAdmin: Boolean = false
    )

    private suspend fun obtainAccess(
        serverUrl: String,
        apiKey: String?,
        username: String?,
        password: String?
    ): Result<ObtainedAuth> {
        if (!apiKey.isNullOrBlank()) {
            val tempApi = createIsolatedApiService(serverUrl, apiKey)
            val cfg = tempApi.getConfig()
            return if (cfg.isSuccessful) {
                Result.success(
                    ObtainedAuth(
                        accessToken = apiKey,
                        mode = PhotoPrismAuthMode.STATIC_TOKEN,
                        previewToken = cfg.body()?.previewToken,
                        downloadToken = cfg.body()?.downloadToken
                    )
                )
            } else {
                Result.failure(Exception("Invalid app password / access token: ${cfg.code()}"))
            }
        }

        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            return Result.failure(Exception("No credentials provided"))
        }

        val unauthenticated = createIsolatedApiService(serverUrl, token = null)
        val sessionBody = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val sessionResp = unauthenticated.createSession(sessionBody)
        if (sessionResp.isSuccessful) {
            val body = sessionResp.body()
            if (body?.requiresPasscode() == true) {
                return Result.failure(
                    Exception(
                        "Two-factor authentication is required. Create an app password in " +
                            "PhotoPrism Settings → Account → Apps and Devices and use that instead."
                    )
                )
            }
            val token = body?.resolvedAccessToken()
            if (!token.isNullOrBlank()) {
                return Result.success(
                    ObtainedAuth(
                        accessToken = token,
                        mode = PhotoPrismAuthMode.SESSION,
                        previewToken = body.config?.previewToken,
                        downloadToken = body.config?.downloadToken,
                        userId = body.user?.uid,
                        userEmail = body.user?.email,
                        isAdmin = body.user?.admin == true
                    )
                )
            }
        } else {
            val err = runCatching { sessionResp.errorBody()?.string() }.getOrNull().orEmpty()
            if (err.contains("\"code\":32") || err.contains("passcode", ignoreCase = true)) {
                return Result.failure(
                    Exception(
                        "Two-factor authentication is required. Create an app password in " +
                            "PhotoPrism Settings → Account → Apps and Devices and use that instead."
                    )
                )
            }
            printDebug("PhotoPrismProvider: session failed ${sessionResp.code()} — $err")
        }

        // Fall back to OAuth2 client credentials (username=client_id, password=client_secret).
        val oauth = unauthenticated.oauthToken(
            grantType = "client_credentials",
            clientId = username,
            clientSecret = password
        )
        if (oauth.isSuccessful) {
            val token = oauth.body()?.accessToken
            if (!token.isNullOrBlank()) {
                val cfgApi = createIsolatedApiService(serverUrl, token)
                val cfg = cfgApi.getConfig()
                return Result.success(
                    ObtainedAuth(
                        accessToken = token,
                        mode = PhotoPrismAuthMode.OAUTH_CLIENT,
                        previewToken = cfg.body()?.previewToken,
                        downloadToken = cfg.body()?.downloadToken
                    )
                )
            }
        }

        val oauthErr = runCatching { oauth.errorBody()?.string() }.getOrNull()
        return Result.failure(
            Exception(
                "Authentication failed (session ${sessionResp.code()}, oauth ${oauth.code()})" +
                    (oauthErr?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "")
            )
        )
    }

    // === Remote Assets ===

    override fun getRemoteAssets(page: Int, pageSize: Int): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val response = requireApi().getPhotos(
                count = pageSize,
                offset = page * pageSize
            )
            if (response.isSuccessful) {
                captureTokensFromHeaders(response.headers())
                val entities = mapPhotos(response.body().orEmpty())
                cloudMediaDao.insertAll(entities)
                emit(Resource.Success(entities))
            } else {
                emit(Resource.Error("Failed to fetch assets: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getRemoteFavorites(): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val response = requireApi().getPhotos(
                count = 1000,
                offset = 0,
                favorite = true
            )
            if (response.isSuccessful) {
                captureTokensFromHeaders(response.headers())
                emit(Resource.Success(mapPhotos(response.body().orEmpty())))
            } else {
                emit(Resource.Error("Failed to fetch favorites: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getRemoteTrashed(): Flow<Resource<List<CloudMediaEntity>>> =
        trashClient.remoteTrashed()

    override fun getRemoteArchived(): Flow<Resource<List<CloudMediaEntity>>> =
        trashClient.remoteArchived()

    // === Albums ===

    override fun getRemoteAlbums(): Flow<Resource<List<CloudAlbum>>> = flow {
        try {
            val configId = currentConfig?.id ?: 0L
            val response = requireApi().getAlbums()
            if (response.isSuccessful) {
                val albums = response.body()?.map { dto ->
                    CloudAlbum(
                        remoteId = dto.uid,
                        providerType = ProviderType.PHOTOPRISM,
                        serverConfigId = configId,
                        name = dto.title,
                        assetCount = dto.photoCount,
                        isShared = dto.linkCount > 0,
                        createdAt = PhotoPrismPhotoDto.parseIsoTimestamp(dto.createdAt ?: ""),
                        updatedAt = PhotoPrismPhotoDto.parseIsoTimestamp(dto.updatedAt ?: "")
                    )
                } ?: emptyList()
                emit(Resource.Success(albums))
            } else {
                emit(Resource.Error("Failed to fetch albums: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getRemoteAlbumMedia(albumId: String): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val response = requireApi().getPhotos(
                count = 1000,
                offset = 0,
                albumUid = albumId
            )
            if (response.isSuccessful) {
                captureTokensFromHeaders(response.headers())
                val entities = mapPhotos(response.body().orEmpty())
                cloudMediaDao.insertAll(entities)
                emit(Resource.Success(entities))
            } else {
                emit(Resource.Error("Failed to fetch album media: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun createAlbum(name: String): Result<CloudAlbum> {
        return try {
            val configId = currentConfig?.id ?: 0L
            val response = requireApi().createAlbum(mapOf("Title" to name))
            if (response.isSuccessful) {
                val dto = response.body()!!
                Result.success(
                    CloudAlbum(
                        remoteId = dto.uid,
                        providerType = ProviderType.PHOTOPRISM,
                        serverConfigId = configId,
                        name = dto.title.ifBlank { name },
                        assetCount = 0
                    )
                )
            } else {
                Result.failure(Exception("Failed to create album: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addToAlbum(albumId: String, assetIds: List<String>): Result<Unit> {
        return try {
            val response = requireApi().addPhotosToAlbum(albumId, mapOf("photos" to assetIds))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to add to album: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(remoteId: String, favorite: Boolean): Result<Unit> {
        return try {
            val response = if (favorite) {
                requireApi().likePhoto(remoteId)
            } else {
                requireApi().unlikePhoto(remoteId)
            }
            if (response.isSuccessful) {
                cloudMediaDao.updateFavorite(remoteId, ProviderType.PHOTOPRISM, currentConfig?.id ?: 0L, favorite)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to toggle favorite: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleArchive(remoteId: String, archived: Boolean): Result<Unit> =
        trashClient.toggleArchive(remoteId, archived)

    override suspend fun trashAsset(remoteId: String): Result<Unit> =
        trashClient.trashAsset(remoteId)

    override suspend fun restoreAsset(remoteId: String): Result<Unit> =
        trashClient.restoreAsset(remoteId)

    // Hard delete / empty trash intentionally unsupported for PhotoPrism.
    override suspend fun deleteAsset(remoteId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("PhotoPrism permanent delete is not supported"))

    override suspend fun emptyTrash(): Result<Unit> =
        Result.failure(UnsupportedOperationException("PhotoPrism empty trash is not supported"))

    override suspend fun restoreAllTrash(): Result<Unit> =
        Result.failure(UnsupportedOperationException("PhotoPrism restore-all trash is not supported"))

    override suspend fun search(query: String): Result<List<CloudMediaEntity>> {
        return try {
            val response = requireApi().getPhotos(
                count = 100,
                offset = 0,
                query = query
            )
            if (response.isSuccessful) {
                captureTokensFromHeaders(response.headers())
                Result.success(mapPhotos(response.body().orEmpty()))
            } else {
                Result.failure(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getThumbnailUrl(remoteId: String, size: ThumbnailSize): String =
        getThumbnailUrl(remoteId, size, hashByRemoteId[remoteId])

    override fun getThumbnailUrl(remoteId: String, size: ThumbnailSize, fileId: String?): String {
        val hash = fileId?.takeIf { it.isNotBlank() }?.also { hashByRemoteId[remoteId] = it }
            ?: hashByRemoteId[remoteId]
            ?: return ""
        val token = authInterceptor.previewToken ?: return ""
        val thumb = when (size) {
            ThumbnailSize.THUMBNAIL -> "tile_224"
            ThumbnailSize.PREVIEW -> "fit_720"
        }
        return "$baseUrl/api/v1/t/$hash/$token/$thumb"
    }

    override fun getOriginalUrl(remoteId: String): String {
        val hash = hashByRemoteId[remoteId] ?: return ""
        val token = authInterceptor.downloadToken ?: return ""
        return "$baseUrl/api/v1/dl/$hash?t=$token"
    }

    override fun getAuthHeaders(): Map<String, String> = buildMap {
        authInterceptor.accessToken?.let { token ->
            put("Authorization", "Bearer $token")
            put("X-Auth-Token", token)
        }
    }

    override suspend fun getServerVersion(): Result<String> = try {
        val response = requireApi().getConfig()
        if (response.isSuccessful) {
            Result.success(response.body()?.version.orEmpty().ifBlank { "unknown" })
        } else {
            Result.failure(Exception("Failed to get server version: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStorageInfo(): Result<CloudStorageInfo> =
        Result.failure(UnsupportedOperationException("PhotoPrism does not expose storage quota via this API"))

    // === Sync / Upload ===

    override suspend fun uploadAsset(localMedia: Media, targetPath: String?): Result<CloudMediaEntity> =
        uploadTarget.uploadAsset(localMedia, contentHash = null)

    override suspend fun uploadAsset(
        localMedia: Media,
        targetPath: String?,
        checksum: String
    ): Result<CloudMediaEntity> = uploadTarget.uploadAsset(localMedia, contentHash = checksum)

    override suspend fun downloadAsset(remoteId: String): Result<Uri> {
        return try {
            val url = getOriginalUrl(remoteId)
            if (url.isBlank()) {
                return Result.failure(Exception("No download URL for $remoteId"))
            }
            val requestBuilder = Request.Builder().url(url).get()
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            val client = CloudFetcherRegistryHolder.okHttpClient
                ?: return Result.failure(Exception("OkHttpClient not initialized"))
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use Result.failure(Exception("Download failed: ${response.code}"))
                }
                val body = response.body
                val ext = when {
                    body.contentType()?.subtype?.contains("jpeg") == true -> ".jpg"
                    body.contentType()?.subtype?.contains("png") == true -> ".png"
                    body.contentType()?.subtype?.contains("mp4") == true -> ".mp4"
                    else -> ""
                }
                val cacheFile = File(context.cacheDir, "pp_download_${remoteId.take(12)}$ext")
                body.byteStream().use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
                Result.success(cacheFile.toUri())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSyncDelta(timestamp: Long, reconcileIndex: Boolean): Result<SyncDelta> {
        // PhotoPrism has no updatedAfter filter and no deletion channel. A newest page
        // is a partial view, so reconcileIndex must not produce a complete remote index.
        printDebug("PhotoPrismProvider: sync delta since $timestamp reconcile=$reconcileIndex")
        return try {
            val response = requireApi().getPhotos(count = 1000, offset = 0, order = "newest")
            if (response.isSuccessful) {
                captureTokensFromHeaders(response.headers())
                val entities = mapPhotos(response.body().orEmpty())
                    .filter { timestamp <= 0L || it.timestamp >= timestamp }
                Result.success(SyncDelta(items = entities))
            } else {
                Result.failure(Exception("Failed to fetch changes: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkUploadCheck(hashes: List<String>): Result<Map<String, Boolean>> =
        uploadTarget.bulkUploadCheck(hashes)

    override suspend fun remoteExists(localMedia: Media, targetPath: String?): Boolean =
        uploadTarget.remoteExists(localMedia)

    override suspend fun afterUploadBatch(): Result<Unit> = uploadTarget.finalizeBatch()
}
