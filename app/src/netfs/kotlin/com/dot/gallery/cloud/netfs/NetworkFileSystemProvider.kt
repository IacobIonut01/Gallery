/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.netfs

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import androidx.core.net.toUri
import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudAuthToken
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.CloudTrace
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
import com.dot.gallery.cloud.netfs.bridge.NetFsLoopback
import com.dot.gallery.cloud.netfs.bridge.NetFsLoopbackSource
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.util.printDebug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Generic network-filesystem [RemoteMediaProvider]. All protocol-agnostic behavior lives
 * here; per-protocol I/O is delegated to a [FileSystemBackend]. SMB and NFS are instances
 * of this class with different backends — mirroring `WebDavMediaProvider` + `WebDavDialect`.
 *
 * Media bytes are exposed to the app's HTTP-only image/video pipeline through the
 * [NetFsLoopback] bridge: [getOriginalUrl]/[getThumbnailUrl] return `http://127.0.0.1` URLs
 * that the bridge resolves back to this provider via [NetFsLoopbackSource].
 */
open class NetworkFileSystemProvider(
    private val context: Context,
    private val cloudMediaDao: CloudMediaDao,
    private val backend: FileSystemBackend
) : RemoteMediaProvider,
    ShareLinkCapableProvider,
    SyncCapableProvider,
    Disconnectable,
    NetFsLoopbackSource {

    override val providerType: ProviderType get() = backend.providerType
    override val displayName: String get() = backend.displayName

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentConfig: CloudServerConfig? = null

    @Volatile
    private var connection: NetFsConnection? = null

    override val isAvailable: Boolean
        get() = currentConfig != null && _connectionState.value == ConnectionState.CONNECTED

    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.REMOTE_ASSETS,
        ProviderCapability.REMOTE_ALBUMS,
        ProviderCapability.SYNC
    )

    override fun configure(config: CloudServerConfig) {
        currentConfig = config
        printDebug("${backend.displayName}Provider: Configured with ${config.serverUrl}")
    }

    override fun disconnect() {
        connection?.let { runCatching { backend.close(it) } }
        connection = null
        _connectionState.value = ConnectionState.DISCONNECTED
        currentConfig = null
    }

    @Synchronized
    private fun requireConnection(): NetFsConnection {
        connection?.let { return it }
        val config = currentConfig ?: throw IllegalStateException("Not configured")
        return CloudTrace.time("${backend.providerType} connect") {
            backend.connect(config)
        }.also { connection = it }
    }

    // === Auth ===

    override suspend fun testConnection(config: CloudServerConfig): Result<CloudServerInfo> =
        withContext(Dispatchers.IO) {
            try {
                val conn = backend.connect(config)
                val info = CloudServerInfo(version = backend.displayName, serverName = conn.rootDisplay)
                runCatching { backend.close(conn) }
                Result.success(info)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun authenticate(config: CloudServerConfig): Result<CloudAuthToken> =
        withContext(Dispatchers.IO) {
            try {
                configure(config)
                requireConnection()
                _connectionState.value = ConnectionState.CONNECTED
                Result.success(CloudAuthToken(accessToken = "", userId = config.username))
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                Result.failure(e)
            }
        }

    override suspend fun getServerVersion(): Result<String> =
        Result.success(backend.displayName)

    // === Remote assets ===

    override fun getRemoteAssets(page: Int, pageSize: Int): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val conn = requireConnection()
            val configId = currentConfig?.id ?: 0L
            val all = scanMedia(conn, "")
            val paged = all.drop(page * pageSize).take(pageSize).map { it.toEntity(configId) }
            cloudMediaDao.insertAll(paged)
            emit(Resource.Success(paged))
        } catch (e: CancellationException) {
            // Consumer cancelled (e.g. a `first()`/`take()` prefetch). Re-throw so cancellation
            // propagates cleanly — emitting from this catch would violate flow exception
            // transparency (AbortFlowException) and abort the whole prefetch.
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getRemoteFavorites(): Flow<Resource<List<CloudMediaEntity>>> =
        flow { emit(Resource.Success(emptyList<CloudMediaEntity>())) }

    override fun getRemoteTrashed(): Flow<Resource<List<CloudMediaEntity>>> =
        flow { emit(Resource.Success(emptyList<CloudMediaEntity>())) }

    override fun getRemoteArchived(): Flow<Resource<List<CloudMediaEntity>>> =
        flow { emit(Resource.Success(emptyList<CloudMediaEntity>())) }

    // === Albums (folders) ===

    override fun getRemoteAlbums(): Flow<Resource<List<CloudAlbum>>> = flow {
        try {
            val conn = requireConnection()
            val configId = currentConfig?.id ?: 0L
            val albums = CloudTrace.time("${backend.providerType} getRemoteAlbums") {
                backend.listDir(conn, "")
                    .filter { it.isDirectory }
                    .map { dir ->
                        CloudAlbum(
                            remoteId = dir.relativePath,
                            providerType = backend.providerType,
                            serverConfigId = configId,
                            name = dir.name,
                            assetCount = 0,
                            isShared = false
                        )
                    }
            }
            CloudTrace.d("${backend.providerType} getRemoteAlbums -> ${albums.size} folders")
            emit(Resource.Success(albums))
        } catch (e: CancellationException) {
            // Consumer cancelled (e.g. a `first()`/`take()` prefetch). Re-throw so cancellation
            // propagates cleanly — emitting from this catch would violate flow exception
            // transparency (AbortFlowException) and abort the whole prefetch.
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getRemoteAlbumMedia(albumId: String): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val conn = requireConnection()
            val configId = currentConfig?.id ?: 0L
            val media = CloudTrace.time("${backend.providerType} getRemoteAlbumMedia '$albumId'") {
                scanMedia(conn, albumId).map { it.toEntity(configId) }
            }
            CloudTrace.d("${backend.providerType} getRemoteAlbumMedia '$albumId' -> ${media.size} items")
            emit(Resource.Success(media))
        } catch (e: CancellationException) {
            // Consumer cancelled (e.g. a `first()`/`take()` prefetch). Re-throw so cancellation
            // propagates cleanly — emitting from this catch would violate flow exception
            // transparency (AbortFlowException) and abort the whole prefetch.
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun createAlbum(name: String): Result<CloudAlbum> = withContext(Dispatchers.IO) {
        try {
            backend.mkdir(requireConnection(), name)
            Result.success(
                CloudAlbum(
                    remoteId = name,
                    providerType = backend.providerType,
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
        Result.failure(UnsupportedOperationException("${backend.displayName} folders don't support adding by id"))

    // === Mutations ===

    override suspend fun toggleFavorite(remoteId: String, favorite: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            // Network shares have no server-side favorites — persist locally only.
            runCatching {
                val configId = currentConfig?.id ?: error("Not configured")
                cloudMediaDao.updateFavorite(remoteId, backend.providerType, configId, favorite)
            }
        }

    override suspend fun toggleArchive(remoteId: String, archived: Boolean): Result<Unit> =
        Result.failure(UnsupportedOperationException("${backend.displayName} does not support archive"))

    override suspend fun trashAsset(remoteId: String): Result<Unit> = deleteAsset(remoteId)

    override suspend fun restoreAsset(remoteId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("${backend.displayName} does not support trash"))

    override suspend fun deleteAsset(remoteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val configId = currentConfig?.id ?: error("Not configured")
            backend.delete(requireConnection(), remoteId)
            cloudMediaDao.delete(remoteId, backend.providerType, configId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun emptyTrash(): Result<Unit> =
        Result.failure(UnsupportedOperationException("${backend.displayName} does not support trash"))

    override suspend fun restoreAllTrash(): Result<Unit> =
        Result.failure(UnsupportedOperationException("${backend.displayName} does not support trash"))

    override suspend fun search(query: String): Result<List<CloudMediaEntity>> = withContext(Dispatchers.IO) {
        try {
            val conn = requireConnection()
            val configId = currentConfig?.id ?: 0L
            val matched = scanMedia(conn, "")
                .filter { it.name.contains(query, ignoreCase = true) }
                .map { it.toEntity(configId) }
            Result.success(matched)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStorageInfo(): Result<CloudStorageInfo> = withContext(Dispatchers.IO) {
        try {
            val storage = backend.storage(requireConnection())
                ?: return@withContext Result.failure(UnsupportedOperationException("No storage info"))
            val pct = if (storage.totalBytes > 0)
                storage.usedBytes.toDouble() / storage.totalBytes.toDouble() * 100.0 else 0.0
            Result.success(
                CloudStorageInfo(
                    usedBytes = storage.usedBytes,
                    totalBytes = storage.totalBytes,
                    usedPercentage = pct,
                    usedFormatted = Formatter.formatShortFileSize(context, storage.usedBytes),
                    totalFormatted = Formatter.formatShortFileSize(context, storage.totalBytes)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === Media-pipeline URLs (loopback bridge) ===

    override fun getThumbnailUrl(remoteId: String, size: ThumbnailSize): String =
        NetFsLoopback.thumbnailUrl(backend.providerType, remoteId, size)

    override fun getOriginalUrl(remoteId: String): String =
        NetFsLoopback.originalUrl(backend.providerType, remoteId)

    override fun getAuthHeaders(): Map<String, String> = emptyMap() // token is embedded in the loopback URL

    // === NetFsLoopbackSource (called from the loopback server thread) ===

    override fun loopbackSize(path: String): Long = backend.fileSize(requireConnection(), path)

    override fun loopbackOpen(path: String, offset: Long): InputStream {
        val fileSize = runCatching { backend.fileSize(requireConnection(), path) }.getOrDefault(-1L)
        val cacheFile = if (fileSize > 0) originalCacheFile(path, fileSize) else null

        // Serve from the on-disk original cache when we already have the complete file: avoids
        // re-downloading the whole multi-MB original on every zoom/range request (traces showed the
        // same 16 MB file streamed repeatedly). Range requests skip into the local file.
        if (cacheFile != null && cacheFile.isFile && cacheFile.length() == fileSize) {
            CloudTrace.d("NetFs original cache HIT '$path' offset=$offset (${CloudTrace.bytes(fileSize)})")
            val fis = FileInputStream(cacheFile)
            if (offset > 0) fis.channel.position(offset)
            return BufferedInputStream(fis, LOOPBACK_READ_BUFFER_BYTES)
        }

        // Buffer with a large window so consumers reading in small chunks (NanoHTTPD streams the
        // original in 16 KB reads; stdlib copy uses 8 KB) still trigger few, large SMB2/NFS READs
        // instead of hundreds of round-trips — the latter timed out the image pipeline.
        val raw = BufferedInputStream(backend.openRead(requireConnection(), path, offset), LOOPBACK_READ_BUFFER_BYTES)

        // Tee a full read (offset 0, bounded size) into the cache. This also warms the cache during
        // thumbnail generation (which reads the whole file), so a later zoom is an instant local read.
        return if (offset == 0L && cacheFile != null && fileSize in 1..ORIGINAL_CACHE_MAX_FILE_BYTES) {
            // Unique temp per stream so concurrent tees of the same file (e.g. thumbnail gen + zoom)
            // don't clobber one another; the first to complete wins the rename, the rest discard.
            val tmp = File(originalCacheDir, "${cacheFile.name}.${System.nanoTime()}.tmp")
            CachingInputStream(raw, cacheFile, tmp, fileSize) { trimOriginalCache() }
        } else raw
    }

    override fun loopbackMime(path: String): String = mimeOf(path)

    override fun loopbackThumbnail(path: String, size: ThumbnailSize): ByteArray? {
        val mime = mimeOf(path)
        val fileSize = runCatching { backend.fileSize(requireConnection(), path) }.getOrDefault(0L)
        val cacheFile = thumbnailCacheFile(path, size, fileSize)

        // Fast path: a previously generated thumbnail. Network filesystems have no server-side
        // preview, so without this every grid cell re-downloads the entire multi-MB original (the
        // 16 s/file seen in traces) — and Glide + Sketch would each generate their own copy.
        cacheFile.takeIf { it.isFile && it.length() > 0 }?.let { f ->
            runCatching { f.readBytes() }.getOrNull()?.let { cached ->
                CloudTrace.d("NetFs thumb cache HIT $size '$path' (${CloudTrace.bytes(cached.size.toLong())})")
                return cached
            }
        }

        // Single-flight per cache key: when Glide and Sketch request the same thumbnail at once,
        // only one thread reads+decodes the original; the rest reuse the just-written cache file.
        val lock = thumbLocks[(cacheFile.name.hashCode() and 0x7fffffff) % thumbLocks.size]
        synchronized(lock) {
            cacheFile.takeIf { it.isFile && it.length() > 0 }?.let { f ->
                runCatching { f.readBytes() }.getOrNull()?.let { return it }
            }
            val bytes = if (mime.startsWith("video/")) {
                NetFsThumbnailer.fromVideoUrl(getOriginalUrl(path), size)
            } else {
                NetFsThumbnailer.fromImage(
                    open = { loopbackOpen(path, 0L) },
                    declaredSize = fileSize,
                    size = size
                )
            }
            if (bytes != null && bytes.isNotEmpty()) {
                runCatching {
                    val tmp = File(thumbCacheDir, "${cacheFile.name}.tmp")
                    tmp.writeBytes(bytes)
                    if (!tmp.renameTo(cacheFile)) {
                        cacheFile.delete(); tmp.renameTo(cacheFile)
                    }
                    CloudTrace.d("NetFs thumb cache STORE $size '$path' (${CloudTrace.bytes(bytes.size.toLong())})")
                }.onFailure { CloudTrace.w("NetFs thumb cache write failed for '$path': ${it.message}") }
            }
            return bytes
        }
    }

    private val thumbCacheDir: File by lazy {
        File(context.cacheDir, "netfs_thumb_cache").apply { mkdirs() }
    }

    /** Stripe locks for single-flight thumbnail generation (bounded, vs. one lock per file). */
    private val thumbLocks = Array(16) { Any() }

    /** Cache file keyed by account + path + size + file size (so edits/replacements invalidate). */
    private fun thumbnailCacheFile(path: String, size: ThumbnailSize, fileSize: Long): File {
        val raw = "${backend.providerType.name}|${currentConfig?.id ?: 0L}|$path|${size.name}|$fileSize"
        return File(thumbCacheDir, sha1(raw) + ".jpg")
    }

    private val originalCacheDir: File by lazy {
        File(context.cacheDir, "netfs_orig_cache").apply { mkdirs() }
    }

    /** Cache file for a full original, keyed by account + path + file size (replacements invalidate). */
    private fun originalCacheFile(path: String, fileSize: Long): File {
        val raw = "${backend.providerType.name}|${currentConfig?.id ?: 0L}|$path|$fileSize"
        return File(originalCacheDir, sha1(raw) + ".bin")
    }

    private fun sha1(s: String): String =
        java.security.MessageDigest.getInstance("SHA-1")
            .digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    /** Evict oldest cached originals (by last access) once the cache exceeds its size cap. */
    @Synchronized
    private fun trimOriginalCache() {
        runCatching {
            val files = originalCacheDir.listFiles()?.filter { it.isFile } ?: return
            var total = files.sumOf { it.length() }
            if (total <= ORIGINAL_CACHE_DIR_CAP_BYTES) return
            files.sortedBy { it.lastModified() }.forEach { f ->
                if (total <= ORIGINAL_CACHE_DIR_CAP_BYTES) return
                total -= f.length()
                f.delete()
            }
        }
    }

    /**
     * Tees a network stream to a temp file as it is read, then atomically promotes it to [target]
     * once the full [expectedSize] has been read (EOF or exact count). A partial/cancelled read
     * discards the temp file, so only complete originals are cached. [onStored] runs after a
     * successful store (used to trim the cache).
     */
    private class CachingInputStream(
        private val delegate: InputStream,
        private val target: File,
        private val tmp: File,
        private val expectedSize: Long,
        private val onStored: () -> Unit
    ) : InputStream() {
        private var out: OutputStream? = runCatching { FileOutputStream(tmp) }.getOrNull()
        private var written = 0L
        private var done = false

        override fun read(): Int {
            val b = delegate.read()
            if (b >= 0) writeByte(b) else finishIfComplete()
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = delegate.read(b, off, len)
            if (n > 0) write(b, off, n) else if (n < 0) finishIfComplete()
            return n
        }

        override fun available(): Int = delegate.available()

        private val one = ByteArray(1)
        private fun writeByte(v: Int) { one[0] = v.toByte(); write(one, 0, 1) }

        private fun write(b: ByteArray, off: Int, n: Int) {
            val o = out ?: return
            try {
                o.write(b, off, n)
                written += n
                if (written >= expectedSize) finishIfComplete()
            } catch (_: Exception) { abort() }
        }

        private fun finishIfComplete() {
            if (done) return
            val o = out ?: return
            out = null
            done = true
            try {
                o.close()
                if (written == expectedSize) {
                    when {
                        target.exists() -> tmp.delete()        // another stream already cached it
                        tmp.renameTo(target) -> {}             // promoted
                        else -> tmp.delete()                   // lost the race / rename failed
                    }
                    onStored()
                } else {
                    tmp.delete()
                }
            } catch (_: Exception) { tmp.delete() }
        }

        private fun abort() {
            done = true
            runCatching { out?.close() }
            out = null
            tmp.delete()
        }

        override fun close() {
            // Cached only if the whole file was read; partial (cancelled/range) reads are discarded.
            if (!done) {
                if (written == expectedSize) finishIfComplete() else abort()
            }
            delegate.close()
        }
    }

    // === Share links (unsupported) ===

    override suspend fun createShareLink(assetIds: List<String>, expiresAt: Long?): Result<String> =
        Result.failure(UnsupportedOperationException("${backend.displayName} does not support share links"))

    override fun getSharedLinks(): Flow<Resource<List<SharedLinkInfo>>> =
        flow { emit(Resource.Success(emptyList())) }

    override suspend fun deleteSharedLink(linkId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("${backend.displayName} does not support share links"))

    override suspend fun updateSharedLink(linkId: String, updates: Map<String, Any>): Result<Unit> =
        Result.failure(UnsupportedOperationException("${backend.displayName} does not support share links"))

    // === Sync ===

    override suspend fun uploadAsset(localMedia: Media, targetPath: String?): Result<CloudMediaEntity> =
        withContext(Dispatchers.IO) {
            try {
                val conn = requireConnection()
                val configId = currentConfig?.id ?: 0L
                val fileName = localMedia.label
                val remotePath = (targetPath?.trimEnd('/')?.ifEmpty { null } ?: "Photos") + "/$fileName"
                val input = context.contentResolver.openInputStream(localMedia.getUri())
                    ?: return@withContext Result.failure(Exception("Cannot open media file"))
                val size = runCatching {
                    context.contentResolver.openAssetFileDescriptor(localMedia.getUri(), "r")?.use { it.length }
                }.getOrNull() ?: -1L
                input.use { backend.write(conn, remotePath, it, size) }
                val entity = CloudMediaEntity(
                    remoteId = remotePath,
                    providerType = backend.providerType,
                    serverConfigId = configId,
                    label = fileName,
                    path = remotePath,
                    relativePath = remotePath.substringBeforeLast('/'),
                    mimeType = localMedia.mimeType,
                    timestamp = System.currentTimeMillis(),
                    size = if (size > 0) size else 0L,
                    syncState = SyncState.SYNCED,
                    localCopyPath = localMedia.getUri().toString()
                )
                cloudMediaDao.insert(entity)
                Result.success(entity)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun downloadAsset(remoteId: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val conn = requireConnection()
            val ext = remoteId.substringAfterLast('.', "")
            val cacheFile = File(context.cacheDir, "netfs_${remoteId.hashCode()}.$ext")
            backend.openRead(conn, remoteId, 0L).use { input ->
                cacheFile.outputStream().use { input.copyTo(it) }
            }
            Result.success(cacheFile.toUri())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChangedSince(timestamp: Long): Result<List<CloudMediaEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val conn = requireConnection()
                val configId = currentConfig?.id ?: 0L
                Result.success(scanMedia(conn, "").map { it.toEntity(configId) })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun bulkUploadCheck(hashes: List<String>): Result<Map<String, Boolean>> =
        Result.success(hashes.associateWith { false })

    /**
     * SMB/NFS have no content-hash index, so an asset is "already uploaded" when a file
     * with the same name and byte size already exists at the deterministic upload target
     * (mirrors the path built by [uploadAsset]). This lets the backup worker skip files
     * it already pushed instead of re-transferring the whole album every run.
     */
    override suspend fun remoteExists(localMedia: Media, targetPath: String?): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val conn = requireConnection()
                val remotePath = (targetPath?.trimEnd('/')?.ifEmpty { null } ?: "Photos") + "/${localMedia.label}"
                val remoteSize = runCatching { backend.fileSize(conn, remotePath) }.getOrNull()
                    ?: return@withContext false
                if (remoteSize <= 0L) return@withContext false
                val localSize = runCatching {
                    context.contentResolver.openAssetFileDescriptor(localMedia.getUri(), "r")?.use { it.length }
                }.getOrNull() ?: return@withContext true // present remotely; can't stat local, treat as done
                remoteSize == localSize
            } catch (_: Exception) {
                false
            }
        }

    // === Helpers ===

    private val mediaExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heif", "heic", "avif",
        "mp4", "mkv", "mov", "avi", "webm", "3gp"
    )

    private fun scanMedia(conn: NetFsConnection, path: String, depth: Int = 0): List<NetFsEntry> {
        if (depth > 4) return emptyList()
        return try {
            backend.listDir(conn, path).flatMap { entry ->
                if (entry.isDirectory) {
                    scanMedia(conn, entry.relativePath, depth + 1)
                } else {
                    val ext = entry.name.substringAfterLast('.', "").lowercase()
                    if (ext in mediaExtensions) listOf(entry) else emptyList()
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun mimeOf(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "heif", "heic" -> "image/heif"
        "avif" -> "image/avif"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        "webm" -> "video/webm"
        "3gp" -> "video/3gpp"
        else -> "application/octet-stream"
    }

    private companion object {
        // ~1 MB: large enough that smbj fills it with a single SMB2 READ (its typical negotiated max),
        // collapsing the per-file round-trips that were timing out the image pipeline.
        const val LOOPBACK_READ_BUFFER_BYTES = 1024 * 1024

        // Don't cache originals larger than this on disk (e.g. long videos) — keep the cache for
        // photos/short clips where a full re-download is the dominant cost.
        const val ORIGINAL_CACHE_MAX_FILE_BYTES = 256L * 1024 * 1024
        // Total on-disk budget for cached originals; oldest are evicted past this.
        const val ORIGINAL_CACHE_DIR_CAP_BYTES = 1024L * 1024 * 1024
    }

    private fun NetFsEntry.toEntity(configId: Long): CloudMediaEntity {
        val ts = if (lastModified > 0) lastModified else System.currentTimeMillis()
        return CloudMediaEntity(
            remoteId = relativePath,
            providerType = backend.providerType,
            serverConfigId = configId,
            label = name,
            path = relativePath,
            relativePath = relativePath.substringBeforeLast('/', ""),
            mimeType = mimeOf(name),
            timestamp = ts,
            size = size,
            syncState = SyncState.REMOTE_ONLY,
            thumbnailUrl = NetFsLoopback.thumbnailUrl(backend.providerType, relativePath, ThumbnailSize.PREVIEW),
            originalUrl = NetFsLoopback.originalUrl(backend.providerType, relativePath)
        )
    }
}
