/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudAuthToken
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.core.Resource
import com.dot.gallery.domain.DomainHooks
import com.dot.gallery.domain.util.IODispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import com.dot.gallery.core.util.or
import com.dot.gallery.domain.uri.query


interface RemoteMediaProvider : MediaCapabilityProvider {
    val connectionState: StateFlow<ConnectionState>

    suspend fun testConnection(config: CloudServerConfig): Result<CloudServerInfo>
    suspend fun authenticate(config: CloudServerConfig): Result<CloudAuthToken>

    fun getRemoteAssets(page: Int, pageSize: Int): Flow<Resource<List<CloudMediaEntity>>>
    fun getRemoteAlbums(): Flow<Resource<List<CloudAlbum>>>
    fun getRemoteAlbumMedia(albumId: String): Flow<Resource<List<CloudMediaEntity>>>
    fun getRemoteFavorites(): Flow<Resource<List<CloudMediaEntity>>>
    fun getRemoteTrashed(): Flow<Resource<List<CloudMediaEntity>>>

    suspend fun toggleFavorite(remoteId: String, favorite: Boolean): Result<Unit>
    suspend fun toggleArchive(remoteId: String, archived: Boolean): Result<Unit>
    suspend fun trashAsset(remoteId: String): Result<Unit>
    suspend fun restoreAsset(remoteId: String): Result<Unit>
    suspend fun deleteAsset(remoteId: String): Result<Unit>
    suspend fun emptyTrash(): Result<Unit>
    suspend fun restoreAllTrash(): Result<Unit>
    suspend fun createAlbum(name: String): Result<CloudAlbum>
    suspend fun addToAlbum(albumId: String, assetIds: List<String>): Result<Unit>
    suspend fun search(query: String): Result<List<CloudMediaEntity>>

    fun getRemoteArchived(): Flow<Resource<List<CloudMediaEntity>>>

    suspend fun getStorageInfo(): Result<CloudStorageInfo>
    suspend fun getServerVersion(): Result<String> = Result.failure(UnsupportedOperationException())

    fun getThumbnailUrl(remoteId: String, size: ThumbnailSize = ThumbnailSize.PREVIEW): String

    /**
     * Thumbnail URL variant that can use the server's stable numeric file id (when
     * available) to request previews the path-based endpoints can't serve — notably
     * video frame previews on ownCloud/Nextcloud's `core/preview` endpoint. Providers
     * that don't need a [fileId] inherit the default and ignore it. May return an empty
     * string when no server preview is available for the item.
     */
    fun getThumbnailUrl(remoteId: String, size: ThumbnailSize, fileId: String?): String =
        getThumbnailUrl(remoteId, size)

    fun getOriginalUrl(remoteId: String): String

    /**
     * Original URL variant that can use a stored file hash / server file id when the
     * in-memory map is cold (e.g. PhotoPrism `Hash` threaded as URI `fileId`). Providers
     * that don't need a [fileId] inherit the default and ignore it.
     */
    fun getOriginalUrl(remoteId: String, fileId: String?): String = getOriginalUrl(remoteId)

    fun getAuthHeaders(): Map<String, String>

    /**
     * Locally-decoded video poster frame as JPEG bytes, for items that have NO server-side
     * preview (path-based stores like SMB/NFS/WebDAV return an empty [getThumbnailUrl] for
     * videos). Implementations decode a frame from the original stream (e.g. via
     * [android.media.MediaMetadataRetriever]). Returns null when unavailable or not a video.
     * Content-addressable stores (e.g. Immich) serve previews over HTTP and keep the default.
     */
    suspend fun getVideoThumbnailBytes(remoteId: String, size: ThumbnailSize): ByteArray? = null

    /**
     * Reads up to [length] bytes of the remote original starting at [offset] via a
     * ranged GET on [getOriginalUrl] with [getAuthHeaders]. The capture-time index
     * uses this to read embedded metadata (EXIF DateTimeOriginal, container
     * creation time) without downloading the whole file — every provider whose
     * originals are HTTP(S)-reachable inherits it for free, including SMB/NFS via
     * their loopback server.
     *
     * Returns null when no original URL exists, the request fails, or the server
     * ignored the Range header on a non-zero [offset] (a mid-file read can't be
     * served from a full-body 200 without transferring the whole prefix). A 200
     * at offset 0 still returns the capped prefix.
     */
    suspend fun fetchRange(remoteId: String, offset: Long, length: Long): ByteArray? =
        withContext(IODispatcher) {
            if (offset < 0L || length <= 0L || length > Int.MAX_VALUE) return@withContext null
            val url = getOriginalUrl(remoteId)
                .takeIf { it.startsWith("http://") || it.startsWith("https://") }
                ?: return@withContext null
            val fetcher = DomainHooks.httpRangeFetcher ?: return@withContext null
            runCatching { fetcher(url, offset, length, getAuthHeaders()) }.getOrNull()
        }

    fun configure(config: CloudServerConfig)
}
