/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.immich

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.dot.gallery.BuildConfig
import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudAuthToken
import com.dot.gallery.cloud.core.CloudMapMarker
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.Disconnectable
import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SharedLinkInfo
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.MapCapableProvider
import com.dot.gallery.cloud.core.capabilities.MemoriesCapableProvider
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.ShareLinkCapableProvider
import com.dot.gallery.cloud.core.capabilities.SmartSearchCapableProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.image.CloudMediaFetcher
import com.dot.gallery.cloud.immich.data.api.ImmichApiService
import com.dot.gallery.cloud.immich.data.api.ImmichAuthInterceptor
import com.dot.gallery.cloud.immich.data.dto.ImmichAssetDto
import com.dot.gallery.cloud.immich.data.dto.ImmichBulkCheckItemDto
import com.dot.gallery.cloud.immich.data.dto.ImmichBulkUploadCheckDto
import com.dot.gallery.cloud.immich.data.dto.ImmichLoginDto
import com.dot.gallery.cloud.immich.data.dto.ImmichSearchDto
import com.dot.gallery.cloud.immich.data.dto.ImmichSharedLinkCreateDto
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.util.printDebug
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Singleton
class ImmichProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authInterceptor: ImmichAuthInterceptor,
    private val cloudMediaDao: CloudMediaDao
) : RemoteMediaProvider,
    MapCapableProvider,
    PeopleCapableProvider,
    SmartSearchCapableProvider,
    ShareLinkCapableProvider,
    SyncCapableProvider,
    MemoriesCapableProvider,
    Disconnectable {

    override val providerType = ProviderType.IMMICH
    override val displayName = "Immich"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentConfig: CloudServerConfig? = null
    private var baseUrl: String = ""
    private var apiService: ImmichApiService? = null

    override val isAvailable: Boolean
        get() = currentConfig != null && _connectionState.value == ConnectionState.CONNECTED

    override fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        currentConfig = null
        apiService = null
        authInterceptor.apiKey = null
        authInterceptor.accessToken = null
        baseUrl = ""
    }

    private fun applyInsecureTls(builder: OkHttpClient.Builder) {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        builder
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
    }

    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.REMOTE_ASSETS,
        ProviderCapability.REMOTE_ALBUMS,
        ProviderCapability.SYNC,
        ProviderCapability.PEOPLE,
        ProviderCapability.MAP,
        ProviderCapability.SMART_SEARCH,
        ProviderCapability.SHARE_LINK,
        ProviderCapability.ARCHIVE,
        ProviderCapability.MEMORIES
    )

    override fun configure(config: CloudServerConfig) {
        currentConfig = config
        baseUrl = config.serverUrl.trimEnd('/')
        authInterceptor.apiKey = config.apiKey
        apiService = createApiService(baseUrl)
        printDebug("ImmichProvider: Configured with server ${config.serverUrl}")
    }

    private fun createApiService(serverUrl: String): ImmichApiService {
        val url = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)

        if (BuildConfig.ALLOW_INSECURE_TLS) {
            applyInsecureTls(clientBuilder)
        }

        val client = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImmichApiService::class.java)
    }

    private fun requireApi(): ImmichApiService = apiService
        ?: throw IllegalStateException("ImmichProvider not configured. Call configure() first.")

    // === Auth ===

    private fun createIsolatedApiService(
        serverUrl: String,
        apiKey: String? = null,
        token: String? = null
    ): ImmichApiService {
        val url = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val tempInterceptor = ImmichAuthInterceptor().apply {
            this.apiKey = apiKey
            this.accessToken = token
        }
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(tempInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.ALLOW_INSECURE_TLS) {
            applyInsecureTls(clientBuilder)
        }
        return Retrofit.Builder()
            .baseUrl(url)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImmichApiService::class.java)
    }

    override suspend fun testConnection(config: CloudServerConfig): Result<CloudServerInfo> {
        return try {
            val tempUrl = config.serverUrl.trimEnd('/')
            val tempApi = createIsolatedApiService(tempUrl, apiKey = config.apiKey)
            val response = tempApi.getServerAbout()
            if (response.isSuccessful) {
                val about = response.body()!!
                val storage = try {
                    tempApi.getServerStorage().body()
                } catch (_: Exception) { null }
                Result.success(
                    CloudServerInfo(
                        version = about.version,
                        serverName = "Immich ${about.version}",
                        storageUsed = storage?.diskUsedRaw ?: 0L,
                        storageTotal = storage?.diskSizeRaw ?: 0L
                    )
                )
            } else {
                Result.failure(Exception("Connection failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun authenticate(config: CloudServerConfig): Result<CloudAuthToken> {
        return try {
            if (!config.apiKey.isNullOrBlank()) {
                authInterceptor.apiKey = config.apiKey
                val validate = requireApi().validateToken()
                if (validate.isSuccessful && validate.body()?.authStatus == true) {
                    val user = requireApi().getCurrentUser().body()
                    _connectionState.value = ConnectionState.CONNECTED
                    Result.success(
                        CloudAuthToken(
                            accessToken = config.apiKey,
                            userId = user?.id,
                            userEmail = user?.email,
                            isAdmin = user?.isAdmin ?: false
                        )
                    )
                } else {
                    _connectionState.value = ConnectionState.ERROR
                    Result.failure(Exception("Invalid API key"))
                }
            } else if (!config.username.isNullOrBlank() && !config.password.isNullOrBlank()) {
                val loginResponse = requireApi().login(
                    ImmichLoginDto(email = config.username, password = config.password)
                )
                if (loginResponse.isSuccessful) {
                    val body = loginResponse.body()!!
                    authInterceptor.accessToken = body.accessToken
                    _connectionState.value = ConnectionState.CONNECTED
                    Result.success(
                        CloudAuthToken(
                            accessToken = body.accessToken,
                            userId = body.userId,
                            userEmail = body.userEmail,
                            isAdmin = body.isAdmin
                        )
                    )
                } else {
                    _connectionState.value = ConnectionState.ERROR
                    Result.failure(Exception("Login failed: ${loginResponse.code()}"))
                }
            } else {
                Result.failure(Exception("No credentials provided"))
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    // === Remote Assets ===

    override fun getRemoteAssets(page: Int, pageSize: Int): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val configId = currentConfig?.id ?: 0L
            val body = mapOf<String, Any>(
                "page" to (page + 1),
                "size" to pageSize,
                "order" to "desc",
                "withExif" to true
            )
            val response = requireApi().searchAssets(body)
            if (response.isSuccessful) {
                val searchResponse = response.body()
                val entities = searchResponse?.assets?.items?.map { it.toCloudMediaEntity(configId, baseUrl) } ?: emptyList()
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
            val configId = currentConfig?.id ?: 0L
            val body = mapOf<String, Any>("isFavorite" to true, "size" to 1000, "withExif" to true)
            val response = requireApi().searchAssets(body)
            if (response.isSuccessful) {
                val entities = response.body()?.assets?.items?.map { it.toCloudMediaEntity(configId, baseUrl) } ?: emptyList()
                emit(Resource.Success(entities))
            } else {
                emit(Resource.Error("Failed to fetch favorites: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getRemoteTrashed(): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val configId = currentConfig?.id ?: 0L
            val body = mapOf<String, Any>("isTrashed" to true, "size" to 1000, "withExif" to true)
            val response = requireApi().searchAssets(body)
            if (response.isSuccessful) {
                val entities = response.body()?.assets?.items?.map { it.toCloudMediaEntity(configId, baseUrl) } ?: emptyList()
                cloudMediaDao.insertAll(entities)
                emit(Resource.Success(entities))
            } else {
                emit(Resource.Error("Failed to fetch trashed: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    // === Albums ===

    override fun getRemoteAlbums(): Flow<Resource<List<CloudAlbum>>> = flow {
        try {
            val configId = currentConfig?.id ?: 0L
            val response = requireApi().getAlbums()
            if (response.isSuccessful) {
                val albums = response.body()?.map { dto ->
                    CloudAlbum(
                        remoteId = dto.id,
                        providerType = ProviderType.IMMICH,
                        serverConfigId = configId,
                        name = dto.albumName,
                        assetCount = dto.assetCount,
                        thumbnailAssetId = dto.albumThumbnailAssetId,
                        isShared = dto.shared,
                        createdAt = ImmichAssetDto.parseIsoTimestamp(dto.createdAt ?: ""),
                        updatedAt = ImmichAssetDto.parseIsoTimestamp(dto.updatedAt ?: "")
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
            val configId = currentConfig?.id ?: 0L
            val response = requireApi().getAlbumById(albumId)
            if (response.isSuccessful) {
                val entities = response.body()?.assets?.map { it.toCloudMediaEntity(configId, baseUrl) } ?: emptyList()
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
            val response = requireApi().createAlbum(mapOf("albumName" to name))
            if (response.isSuccessful) {
                val dto = response.body()!!
                Result.success(
                    CloudAlbum(
                        remoteId = dto.id,
                        providerType = ProviderType.IMMICH,
                        serverConfigId = configId,
                        name = dto.albumName,
                        assetCount = 0,
                        isShared = false
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
            val response = requireApi().addAssetsToAlbum(albumId, mapOf("ids" to assetIds))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to add to album: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(remoteId: String, favorite: Boolean): Result<Unit> {
        return try {
            val response = requireApi().updateAsset(remoteId, mapOf("isFavorite" to favorite))
            if (response.isSuccessful) {
                cloudMediaDao.updateFavorite(remoteId, ProviderType.IMMICH, favorite)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to toggle favorite: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun trashAsset(remoteId: String): Result<Unit> {
        return try {
            val response = requireApi().deleteAssets(mapOf("ids" to listOf(remoteId), "force" to false))
            if (response.isSuccessful) {
                cloudMediaDao.updateTrashed(remoteId, ProviderType.IMMICH, true)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to trash asset: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreAsset(remoteId: String): Result<Unit> {
        return try {
            val response = requireApi().restoreAssets(mapOf("ids" to listOf(remoteId)))
            if (response.isSuccessful) {
                cloudMediaDao.updateTrashed(remoteId, ProviderType.IMMICH, false)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to restore asset: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAsset(remoteId: String): Result<Unit> {
        return try {
            val response = requireApi().deleteAssets(mapOf("ids" to listOf(remoteId), "force" to true))
            if (response.isSuccessful) {
                cloudMediaDao.delete(remoteId, ProviderType.IMMICH)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete asset: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun search(query: String): Result<List<CloudMediaEntity>> {
        return try {
            val configId = currentConfig?.id ?: 0L
            val response = requireApi().metadataSearch(
                mapOf("originalFileName" to query, "page" to 1, "size" to 100, "withExif" to true)
            )
            if (response.isSuccessful) {
                val items = response.body()?.assets?.items?.map { it.toCloudMediaEntity(configId, baseUrl) } ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getThumbnailUrl(remoteId: String, size: ThumbnailSize): String {
        val sizeParam = when (size) {
            ThumbnailSize.THUMBNAIL -> "thumbnail"
            ThumbnailSize.PREVIEW -> "preview"
        }
        return "$baseUrl/api/assets/$remoteId/thumbnail?size=$sizeParam"
    }

    override fun getOriginalUrl(remoteId: String): String = "$baseUrl/api/assets/$remoteId/original"

    override fun getAuthHeaders(): Map<String, String> = buildMap {
        authInterceptor.apiKey?.let { put("x-api-key", it) }
        authInterceptor.accessToken?.let {
            if (authInterceptor.apiKey == null) put("Authorization", "Bearer $it")
        }
    }

    override suspend fun getServerVersion(): Result<String> = try {
        val response = requireApi().getServerAbout()
        if (response.isSuccessful) {
            Result.success(response.body()!!.version)
        } else {
            Result.failure(Exception("Failed to get server version: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStorageInfo(): Result<CloudStorageInfo> = try {
        val response = requireApi().getServerStorage()
        if (response.isSuccessful) {
            val dto = response.body()!!
            printDebug("ImmichProvider: Storage info: used=${dto.diskUsed}, total=${dto.diskSize}, pct=${dto.diskUsedPercentage}")
            Result.success(
                CloudStorageInfo(
                    usedBytes = dto.diskUsedRaw,
                    totalBytes = dto.diskSizeRaw,
                    usedPercentage = dto.diskUsedPercentage,
                    usedFormatted = dto.diskUsed,
                    totalFormatted = dto.diskSize
                )
            )
        } else {
            printDebug("ImmichProvider: Storage info failed: ${response.code()} ${response.message()} body=${response.errorBody()?.string()}")
            Result.failure(Exception("Failed to get storage info: ${response.code()}"))
        }
    } catch (e: Exception) {
        printDebug("ImmichProvider: Storage info exception: ${e.message}")
        Result.failure(e)
    }

    // === People ===

    override fun getPeople(): Flow<Resource<List<PersonInfo>>> = flow {
        try {
            val response = requireApi().getPeople()
            if (response.isSuccessful) {
                val people = response.body()?.people?.filter { !it.isHidden }?.map { dto ->
                    PersonInfo(
                        id = dto.id,
                        name = dto.name,
                        providerType = ProviderType.IMMICH,
                        thumbnailUrl = CloudMediaFetcher.buildPersonUri(ProviderType.IMMICH, dto.id),
                        assetCount = 0,
                        birthDate = dto.birthDate
                    )
                } ?: emptyList()
                emit(Resource.Success(people))
            } else {
                emit(Resource.Error("Failed to fetch people: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getPersonMedia(personId: String): Flow<Resource<List<Media>>> = flow {
        try {
            val configId = currentConfig?.id ?: 0L
            val allMedia = mutableListOf<Media>()
            var page = 1
            var hasMore = true
            while (hasMore) {
                val body = mapOf<String, Any>(
                    "personIds" to listOf(personId),
                    "page" to page,
                    "size" to 200,
                    "order" to "desc",
                    "withExif" to true
                )
                val response = requireApi().searchAssets(body)
                if (response.isSuccessful) {
                    val items = response.body()?.assets?.items ?: emptyList()
                    allMedia.addAll(items.map { dto ->
                        dto.toCloudMediaEntity(configId, baseUrl).toUriMedia()
                    })
                    hasMore = items.size >= 200
                    page++
                } else {
                    emit(Resource.Error("Failed to fetch person media: ${response.code()}"))
                    return@flow
                }
            }
            emit(Resource.Success(allMedia.toList()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getPersonThumbnailUrl(personId: String): String =
        "$baseUrl/api/people/$personId/thumbnail"

    // === Map ===

    override fun getMapMarkers(): Flow<Resource<List<CloudMapMarker>>> = flow {
        try {
            val response = requireApi().getMapMarkers()
            if (response.isSuccessful) {
                val markers = response.body()?.map { dto ->
                    CloudMapMarker(
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        assetId = dto.id,
                        providerType = ProviderType.IMMICH,
                        city = dto.city,
                        country = dto.country
                    )
                } ?: emptyList()
                emit(Resource.Success(markers))
            } else {
                emit(Resource.Error("Failed to fetch map markers: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    // === Smart Search ===

    override suspend fun smartSearch(query: String): Result<List<Media>> {
        return try {
            val configId = currentConfig?.id ?: 0L
            val response = requireApi().smartSearch(ImmichSearchDto(query = query))
            if (response.isSuccessful) {
                val media = response.body()?.assets?.items
                    ?.map { it.toCloudMediaEntity(configId, baseUrl).toUriMedia() }
                    ?: emptyList()
                Result.success(media)
            } else {
                Result.failure(Exception("Smart search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === Share Link ===

    override suspend fun createShareLink(assetIds: List<String>, expiresAt: Long?): Result<String> {
        return try {
            val expiresStr = expiresAt?.let {
                java.time.Instant.ofEpochMilli(it).toString()
            }
            val response = requireApi().createSharedLink(
                ImmichSharedLinkCreateDto(
                    assetIds = assetIds,
                    expiresAt = expiresStr
                )
            )
            if (response.isSuccessful) {
                val key = response.body()?.key ?: ""
                Result.success("$baseUrl/share/$key")
            } else {
                Result.failure(Exception("Failed to create share link: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === Sync ===

    override suspend fun uploadAsset(localMedia: Media, targetPath: String?): Result<CloudMediaEntity> {
        return try {
            val configId = currentConfig?.id ?: 0L
            val mediaUri = localMedia.getUri()
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(mediaUri)
                ?: return Result.failure(Exception("Cannot open media file"))
            val mimeType = localMedia.mimeType
            val fileName = localMedia.label
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
            try {
                inputStream.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
                val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("assetData", fileName, requestBody)
                val deviceAssetId = localMedia.id.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val deviceId = "android-gallery".toRequestBody("text/plain".toMediaTypeOrNull())
                val createdIso = java.time.Instant.ofEpochSecond(localMedia.definedTimestamp).toString()
                val modifiedIso = java.time.Instant.ofEpochSecond(localMedia.timestamp).toString()
                val fileCreatedAt = createdIso.toRequestBody("text/plain".toMediaTypeOrNull())
                val fileModifiedAt = modifiedIso.toRequestBody("text/plain".toMediaTypeOrNull())
                val response = requireApi().uploadAsset(
                    file = filePart,
                    deviceAssetId = deviceAssetId,
                    deviceId = deviceId,
                    fileCreatedAt = fileCreatedAt,
                    fileModifiedAt = fileModifiedAt
                )
                if (response.isSuccessful) {
                    val dto = response.body()!!
                    val entity = dto.toCloudMediaEntity(configId, baseUrl)
                    cloudMediaDao.insert(entity)
                    Result.success(entity)
                } else {
                    Result.failure(Exception("Upload failed: ${response.code()} ${response.message()}"))
                }
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadAsset(remoteId: String): Result<Uri> {
        return try {
            val url = getOriginalUrl(remoteId)
            val authHeaders = getAuthHeaders()
            val requestBuilder = Request.Builder().url(url).get()
            authHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            val client = com.dot.gallery.cloud.image.CloudFetcherRegistryHolder.okHttpClient
                ?: return Result.failure(Exception("OkHttpClient not initialized"))
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Download failed: ${response.code}"))
            }
            val body = response.body ?: return Result.failure(Exception("Empty response"))
            val ext = when {
                body.contentType()?.subtype?.contains("jpeg") == true -> ".jpg"
                body.contentType()?.subtype?.contains("png") == true -> ".png"
                body.contentType()?.subtype?.contains("mp4") == true -> ".mp4"
                else -> ""
            }
            val cacheFile = File(context.cacheDir, "download_${remoteId.take(12)}$ext")
            body.byteStream().use { input -> cacheFile.outputStream().use { output -> input.copyTo(output) } }
            Result.success(cacheFile.toUri())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChangedSince(timestamp: Long): Result<List<CloudMediaEntity>> {
        return try {
            val configId = currentConfig?.id ?: 0L
            val isoTime = java.time.Instant.ofEpochMilli(timestamp).toString()
            val body = mapOf<String, Any>(
                "updatedAfter" to isoTime,
                "size" to 1000,
                "withExif" to true
            )
            val response = requireApi().searchAssets(body)
            if (response.isSuccessful) {
                val entities = response.body()?.assets?.items
                    ?.map { it.toCloudMediaEntity(configId, baseUrl) }
                    ?: emptyList()
                Result.success(entities)
            } else {
                Result.failure(Exception("Failed to fetch changes: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkUploadCheck(hashes: List<String>): Result<Map<String, Boolean>> {
        return try {
            val items = hashes.mapIndexed { i, hash ->
                ImmichBulkCheckItemDto(id = i.toString(), checksum = hash)
            }
            val response = requireApi().bulkUploadCheck(ImmichBulkUploadCheckDto(assets = items))
            if (response.isSuccessful) {
                val results = response.body()?.results?.associate { it.id to (it.action == "reject") } ?: emptyMap()
                Result.success(results)
            } else {
                Result.failure(Exception("Bulk check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === Archive ===

    override suspend fun toggleArchive(remoteId: String, archived: Boolean): Result<Unit> {
        return try {
            val visibility = if (archived) "archive" else "timeline"
            val response = requireApi().updateAsset(remoteId, mapOf("visibility" to visibility))
            if (response.isSuccessful) {
                cloudMediaDao.updateArchived(remoteId, ProviderType.IMMICH, archived)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to toggle archive: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRemoteArchived(): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val configId = currentConfig?.id ?: 0L
            val body = mapOf<String, Any>("visibility" to "archive", "size" to 1000, "withExif" to true)
            val response = requireApi().searchAssets(body)
            if (response.isSuccessful) {
                val entities = response.body()?.assets?.items
                    ?.map { it.toCloudMediaEntity(configId, baseUrl) }
                    ?: emptyList()
                emit(Resource.Success(entities))
            } else {
                emit(Resource.Error("Failed to fetch archived: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    // === Trash Bulk Operations ===

    override suspend fun emptyTrash(): Result<Unit> {
        return try {
            val response = requireApi().emptyTrash()
            if (response.isSuccessful) {
                cloudMediaDao.deleteByProvider(ProviderType.IMMICH)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to empty trash: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreAllTrash(): Result<Unit> {
        return try {
            val response = requireApi().restoreAllTrash()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to restore all trash: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === Shared Links Management ===

    override fun getSharedLinks(): Flow<Resource<List<SharedLinkInfo>>> = flow {
        try {
            val response = requireApi().getSharedLinks()
            if (response.isSuccessful) {
                val links = response.body()?.map { dto ->
                    SharedLinkInfo(
                        id = dto.id,
                        key = dto.key,
                        type = dto.type,
                        description = dto.description,
                        expiresAt = dto.expiresAt?.let { ImmichAssetDto.parseIsoTimestamp(it) },
                        allowDownload = dto.allowDownload,
                        allowUpload = dto.allowUpload,
                        showMetadata = dto.showMetadata,
                        password = dto.password,
                        assetCount = dto.assets.size,
                        providerType = ProviderType.IMMICH,
                        createdAt = dto.createdAt?.let { ImmichAssetDto.parseIsoTimestamp(it) } ?: 0L,
                        thumbnailAssetId = dto.album?.albumThumbnailAssetId
                            ?: dto.assets.firstOrNull()?.id,
                        albumId = dto.album?.id,
                        albumName = dto.album?.albumName
                    )
                } ?: emptyList()
                emit(Resource.Success(links))
            } else {
                emit(Resource.Error("Failed to fetch shared links: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun deleteSharedLink(linkId: String): Result<Unit> {
        return try {
            val response = requireApi().deleteSharedLink(linkId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to delete shared link: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSharedLink(linkId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val response = requireApi().updateSharedLink(linkId, updates)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to update shared link: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === People Editing ===

    override suspend fun updatePersonName(personId: String, name: String): Result<Unit> {
        return try {
            val response = requireApi().updatePerson(personId, mapOf("name" to name))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to update person name: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePersonBirthDate(personId: String, birthDate: String): Result<Unit> {
        return try {
            val response = requireApi().updatePerson(personId, mapOf("birthDate" to birthDate))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to update person birth date: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === Memories ===

    override fun getMemories(): Flow<Resource<List<MemoryInfo>>> = flow {
        try {
            val configId = currentConfig?.id ?: 0L
            val response = requireApi().getMemories()
            if (response.isSuccessful) {
                val memories = response.body()?.map { dto ->
                    val assetMedia = dto.assets.map { asset ->
                        asset.toCloudMediaEntity(configId, baseUrl).toUriMedia()
                    }
                    MemoryInfo(
                        id = dto.id,
                        type = dto.type,
                        year = dto.data?.year ?: 0,
                        assetCount = dto.assets.size,
                        providerType = ProviderType.IMMICH,
                        createdAt = dto.createdAt?.let { ImmichAssetDto.parseIsoTimestamp(it) } ?: 0L,
                        seenAt = dto.seenAt?.let { ImmichAssetDto.parseIsoTimestamp(it) },
                        media = assetMedia
                    )
                } ?: emptyList()
                emit(Resource.Success(memories))
            } else {
                emit(Resource.Error("Failed to fetch memories: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }
}
