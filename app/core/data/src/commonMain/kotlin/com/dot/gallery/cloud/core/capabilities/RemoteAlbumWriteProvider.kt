/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.domain.model.Media
import kotlinx.serialization.Serializable

fun remoteCopyFileName(original: String, copyNumber: Int): String {
    require(copyNumber > 0)
    val dot = original.lastIndexOf('.').takeIf { it > 0 } ?: original.length
    return original.substring(0, dot) + " ($copyNumber)" + original.substring(dot)
}

fun remoteAlbumFilePath(remoteAlbumId: String, fileName: String): String {
    val album = remoteAlbumId.trim('/')
    require(album.isNotBlank() && album.split('/').none { it == "." || it == ".." })
    require(fileName.isNotBlank() && '/' !in fileName && '\\' !in fileName)
    return "$album/$fileName"
}

interface RemoteAlbumWriteProvider : SyncCapableProvider {
    suspend fun copyToAlbum(
        media: Media,
        remoteAlbumId: String,
        conflictPolicy: RemoteNameConflictPolicy = RemoteNameConflictPolicy.KEEP_BOTH,
        checksum: String? = null,
        continuationRemoteId: String? = null
    ): RemoteAlbumCopyResult

    /**
     * Rename (and otherwise update) an existing remote album. Providers without
     * an album-update endpoint keep the default — callers must treat the failure
     * as "unsupported", not retryable.
     */
    suspend fun renameAlbum(remoteAlbumId: String, newName: String): Result<CloudAlbum> =
        Result.failure(UnsupportedOperationException("renameAlbum is not supported by $providerType"))

    /**
     * Delete the remote album itself (member assets are not deleted). Default =
     * unsupported.
     */
    suspend fun deleteRemoteAlbum(remoteAlbumId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("deleteRemoteAlbum is not supported by $providerType"))

    /**
     * Remove assets from a remote album without deleting the assets themselves.
     * Default = unsupported.
     */
    suspend fun removeFromAlbum(remoteAlbumId: String, assetIds: List<String>): Result<Unit> =
        Result.failure(UnsupportedOperationException("removeFromAlbum is not supported by $providerType"))

    /**
     * Replace the album's collaborator (shared-with) list. Default = unsupported.
     */
    suspend fun updateAlbumUsers(remoteAlbumId: String, users: List<RemoteAlbumShare>): Result<CloudAlbum> =
        Result.failure(UnsupportedOperationException("updateAlbumUsers is not supported by $providerType"))
}
