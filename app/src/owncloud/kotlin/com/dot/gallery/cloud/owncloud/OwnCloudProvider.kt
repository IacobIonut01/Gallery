/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.owncloud

import android.net.Uri
import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudAuthToken
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.CloudStorageInfo
import android.content.Context
import androidx.core.net.toUri
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.Disconnectable
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SharedLinkInfo
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.ShareLinkCapableProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.owncloud.data.api.OcsApiClient
import com.dot.gallery.cloud.owncloud.data.api.WebDavClient
import com.dot.gallery.cloud.owncloud.data.api.WebDavResource
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.util.printDebug
import com.dot.gallery.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Singleton
class OwnCloudProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cloudMediaDao: CloudMediaDao
) : RemoteMediaProvider,
    ShareLinkCapableProvider,
    SyncCapableProvider,
    Disconnectable {

    override val providerType = ProviderType.OWNCLOUD
    override val displayName = "ownCloud"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentConfig: CloudServerConfig? = null
    private var webDavClient: WebDavClient? = null
    private var ocsClient: OcsApiClient? = null
    private var baseUrl: String = ""

    override val isAvailable: Boolean
        get() = currentConfig != null && _connectionState.value == ConnectionState.CONNECTED

    override fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        currentConfig = null
        webDavClient = null
        ocsClient = null
        baseUrl = ""
    }

    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.REMOTE_ASSETS,
        ProviderCapability.REMOTE_ALBUMS,
        ProviderCapability.SYNC,
        ProviderCapability.SHARE_LINK
    )

    override fun configure(config: CloudServerConfig) {
        currentConfig = config
        baseUrl = config.serverUrl.trimEnd('/')
        val username = config.username ?: ""
        val password = config.password ?: ""
        val okHttpBuilder = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        if (BuildConfig.ALLOW_INSECURE_TLS) {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            okHttpBuilder
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
        }
        val okHttp = okHttpBuilder.build()
        webDavClient = WebDavClient(okHttp, baseUrl, username, password)
        ocsClient = OcsApiClient(okHttp, baseUrl, username, password)
        printDebug("OwnCloudProvider: Configured with server $baseUrl")
    }

    // === Auth ===

    override suspend fun testConnection(config: CloudServerConfig): Result<CloudServerInfo> =
        withContext(Dispatchers.IO) {
            try {
                val tempOkHttp = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val tempWebDav = WebDavClient(
                    tempOkHttp, config.serverUrl, config.username ?: "", config.password ?: ""
                )
                val tempOcs = OcsApiClient(
                    tempOkHttp, config.serverUrl, config.username ?: "", config.password ?: ""
                )
                if (tempWebDav.testConnection()) {
                    val caps = try { tempOcs.getCapabilities() } catch (_: Exception) { null }
                    Result.success(
                        CloudServerInfo(
                            version = caps?.version ?: "unknown",
                            serverName = "ownCloud ${caps?.versionString ?: ""}".trim()
                        )
                    )
                } else {
                    Result.failure(Exception("WebDAV connection failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun authenticate(config: CloudServerConfig): Result<CloudAuthToken> =
        withContext(Dispatchers.IO) {
            try {
                configure(config)
                if (webDavClient?.testConnection() == true) {
                    _connectionState.value = ConnectionState.CONNECTED
                    val userInfo = try { ocsClient?.getCurrentUser() } catch (_: Exception) { null }
                    Result.success(
                        CloudAuthToken(
                            accessToken = "",
                            userId = userInfo?.id ?: config.username,
                            userEmail = userInfo?.email
                        )
                    )
                } else {
                    _connectionState.value = ConnectionState.ERROR
                    Result.failure(Exception("Authentication failed"))
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                Result.failure(e)
            }
        }

    // === Remote Assets ===

    override fun getRemoteAssets(page: Int, pageSize: Int): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val client = webDavClient ?: throw IllegalStateException("Not configured")
            val configId = currentConfig?.id ?: 0L
            val mediaFiles = scanMediaFiles(client, "")
            val startIdx = page * pageSize
            val paged = mediaFiles.drop(startIdx).take(pageSize)
            val entities = paged.map { it.toCloudMediaEntity(configId) }
            cloudMediaDao.insertAll(entities)
            emit(Resource.Success(entities))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getRemoteFavorites(): Flow<Resource<List<CloudMediaEntity>>> =
        flow { emit(Resource.Success(emptyList<CloudMediaEntity>())) }

    override fun getRemoteTrashed(): Flow<Resource<List<CloudMediaEntity>>> =
        flow { emit(Resource.Success(emptyList<CloudMediaEntity>())) }

    // === Albums / Folders ===

    override fun getRemoteAlbums(): Flow<Resource<List<CloudAlbum>>> = flow {
        try {
            val client = webDavClient ?: throw IllegalStateException("Not configured")
            val configId = currentConfig?.id ?: 0L
            val resources = client.propFind("", depth = 1)
            val folders = resources.filter { it.isCollection && it.href != "/" }
                .map { res ->
                    CloudAlbum(
                        remoteId = res.ownCloudFileId.ifBlank { res.href },
                        providerType = ProviderType.OWNCLOUD,
                        serverConfigId = configId,
                        name = res.displayName,
                        assetCount = 0,
                        isShared = false
                    )
                }
            emit(Resource.Success(folders))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getRemoteAlbumMedia(albumId: String): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val client = webDavClient ?: throw IllegalStateException("Not configured")
            val configId = currentConfig?.id ?: 0L
            val mediaFiles = scanMediaFiles(client, albumId)
            val entities = mediaFiles.map { it.toCloudMediaEntity(configId) }
            emit(Resource.Success(entities))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun createAlbum(name: String): Result<CloudAlbum> =
        withContext(Dispatchers.IO) {
            try {
                webDavClient?.mkdir(name) ?: throw IllegalStateException("Not configured")
                Result.success(
                    CloudAlbum(
                        remoteId = name,
                        providerType = ProviderType.OWNCLOUD,
                        serverConfigId = currentConfig?.id ?: 0L,
                        name = name,
                        assetCount = 0
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun addToAlbum(albumId: String, assetIds: List<String>): Result<Unit> =
        Result.failure(UnsupportedOperationException("ownCloud folders don't support adding by asset ID"))

    override suspend fun toggleFavorite(remoteId: String, favorite: Boolean): Result<Unit> =
        Result.failure(UnsupportedOperationException("ownCloud does not support favorites"))

    override suspend fun trashAsset(remoteId: String): Result<Unit> =
        deleteAsset(remoteId)

    override suspend fun restoreAsset(remoteId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("ownCloud does not support restoring from trash"))

    override suspend fun deleteAsset(remoteId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                webDavClient?.delete(remoteId) ?: throw IllegalStateException("Not configured")
                cloudMediaDao.delete(remoteId, ProviderType.OWNCLOUD)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun search(query: String): Result<List<CloudMediaEntity>> {
        return try {
            val client = webDavClient ?: throw IllegalStateException("Not configured")
            val configId = currentConfig?.id ?: 0L
            val allMedia = scanMediaFiles(client, "")
            val matched = allMedia.filter {
                it.displayName.contains(query, ignoreCase = true)
            }.map { it.toCloudMediaEntity(configId) }
            Result.success(matched)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStorageInfo(): Result<CloudStorageInfo> {
        return Result.failure(UnsupportedOperationException("Storage info not yet implemented for ownCloud"))
    }

    override fun getThumbnailUrl(remoteId: String, size: ThumbnailSize): String {
        val dim = when (size) {
            ThumbnailSize.THUMBNAIL -> 256
            ThumbnailSize.PREVIEW -> 1024
        }
        return webDavClient?.getPreviewUrl(remoteId, dim, dim) ?: ""
    }

    override fun getOriginalUrl(remoteId: String): String =
        webDavClient?.getDownloadUrl(remoteId) ?: ""

    override fun getAuthHeaders(): Map<String, String> =
        webDavClient?.getAuthHeaders() ?: emptyMap()

    // === Share Link ===

    override suspend fun createShareLink(assetIds: List<String>, expiresAt: Long?): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val client = ocsClient ?: throw IllegalStateException("Not configured")
                val path = assetIds.firstOrNull() ?: return@withContext Result.failure(Exception("No path"))
                val expDate = expiresAt?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate().toString()
                }
                val share = client.createPublicShare(path, expirationDate = expDate)
                Result.success(share.url)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // === Sync ===

    override suspend fun uploadAsset(
        localMedia: com.dot.gallery.feature_node.domain.model.Media,
        targetPath: String?
    ): Result<CloudMediaEntity> = withContext(Dispatchers.IO) {
        try {
            val client = webDavClient ?: throw IllegalStateException("Not configured")
            val configId = currentConfig?.id ?: 0L
            val contentResolver = context.contentResolver
            val mediaUri = localMedia.getUri()
            val inputStream = contentResolver.openInputStream(mediaUri)
                ?: return@withContext Result.failure(Exception("Cannot open media file"))
            val fileName = localMedia.label
            val remotePath = (targetPath?.trimEnd('/') ?: "Photos") + "/$fileName"
            val mimeType = localMedia.mimeType
            val tempFile = File(context.cacheDir, "oc_upload_${System.currentTimeMillis()}_$fileName")
            try {
                inputStream.use { i -> tempFile.outputStream().use { o -> i.copyTo(o) } }
                client.upload(remotePath, tempFile, mimeType)
                val entity = CloudMediaEntity(
                    remoteId = remotePath,
                    providerType = ProviderType.OWNCLOUD,
                    serverConfigId = configId,
                    label = fileName,
                    path = remotePath,
                    relativePath = remotePath.substringBeforeLast('/'),
                    mimeType = mimeType,
                    timestamp = System.currentTimeMillis(),
                    size = tempFile.length(),
                    syncState = SyncState.SYNCED
                )
                cloudMediaDao.insert(entity)
                Result.success(entity)
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadAsset(remoteId: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val client = webDavClient ?: throw IllegalStateException("Not configured")
            val bytes = client.download(remoteId)
            val ext = remoteId.substringAfterLast('.', "")
            val cacheFile = File(context.cacheDir, "oc_download_${remoteId.hashCode()}.$ext")
            cacheFile.writeBytes(bytes)
            Result.success(cacheFile.toUri())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChangedSince(timestamp: Long): Result<List<CloudMediaEntity>> {
        return try {
            val client = webDavClient ?: throw IllegalStateException("Not configured")
            val configId = currentConfig?.id ?: 0L
            val allMedia = scanMediaFiles(client, "")
            Result.success(allMedia.map { it.toCloudMediaEntity(configId) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkUploadCheck(hashes: List<String>): Result<Map<String, Boolean>> =
        Result.success(hashes.associateWith { false })

    // === Archive (unsupported) ===

    override suspend fun toggleArchive(remoteId: String, archived: Boolean): Result<Unit> =
        Result.failure(UnsupportedOperationException("ownCloud does not support archive"))

    override fun getRemoteArchived(): Flow<Resource<List<CloudMediaEntity>>> =
        flow { emit(Resource.Success(emptyList())) }

    // === Trash bulk (unsupported) ===

    override suspend fun emptyTrash(): Result<Unit> =
        Result.failure(UnsupportedOperationException("ownCloud does not support empty trash"))

    override suspend fun restoreAllTrash(): Result<Unit> =
        Result.failure(UnsupportedOperationException("ownCloud does not support restore all trash"))

    // === Shared Links Management (unsupported stubs) ===

    override fun getSharedLinks(): Flow<Resource<List<SharedLinkInfo>>> =
        flow { emit(Resource.Success(emptyList())) }

    override suspend fun deleteSharedLink(linkId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("ownCloud does not support shared link deletion via this API"))

    override suspend fun updateSharedLink(linkId: String, updates: Map<String, Any>): Result<Unit> =
        Result.failure(UnsupportedOperationException("ownCloud does not support shared link updates via this API"))

    // === Helpers ===

    private val mediaExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heif", "heic", "avif",
        "mp4", "mkv", "mov", "avi", "webm", "3gp"
    )

    private fun scanMediaFiles(client: WebDavClient, path: String, depth: Int = 0): List<WebDavResource> {
        if (depth > 3) return emptyList()
        return try {
            val resources = client.propFind(path, depth = 1)
            resources.drop(if (path.isBlank()) 1 else 1).flatMap { res ->
                if (res.isCollection) {
                    val subPath = res.href.substringAfter("/remote.php/dav/files/")
                        .substringAfter("/", "")
                    if (subPath.isNotBlank()) scanMediaFiles(client, subPath, depth + 1)
                    else emptyList()
                } else {
                    val ext = res.displayName.substringAfterLast('.', "").lowercase()
                    if (ext in mediaExtensions) listOf(res)
                    else emptyList()
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun WebDavResource.toCloudMediaEntity(configId: Long): CloudMediaEntity {
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val mimeType = when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "heif", "heic" -> "image/heif"
            "avif" -> "image/avif"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"
            else -> contentType.ifBlank { "application/octet-stream" }
        }
        val remotePath = href.substringAfter("/remote.php/dav/files/")
            .substringAfter("/", "")
        val ts = try {
            java.time.ZonedDateTime.parse(
                lastModified,
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
            ).toInstant().toEpochMilli()
        } catch (_: Exception) { System.currentTimeMillis() }

        return CloudMediaEntity(
            remoteId = ownCloudFileId.ifBlank { remotePath },
            providerType = ProviderType.OWNCLOUD,
            serverConfigId = configId,
            label = displayName,
            path = remotePath,
            relativePath = remotePath.substringBeforeLast('/'),
            mimeType = mimeType,
            timestamp = ts,
            size = contentLength,
            syncState = SyncState.REMOTE_ONLY,
            contentHash = etag.trim('"'),
            thumbnailUrl = webDavClient?.getPreviewUrl(ownCloudFileId) ?: "",
            originalUrl = webDavClient?.getDownloadUrl(remotePath) ?: ""
        )
    }
}
