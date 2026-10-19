/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.domain.model.Media
import com.dot.gallery.core.util.or

interface SyncCapableProvider : MediaCapabilityProvider {
    val maxConcurrentUploads: Int get() = 1
    val requiresUploadChecksum: Boolean get() = false

    suspend fun uploadAsset(
        localMedia: Media,
        targetPath: String? = null
    ): Result<CloudMediaEntity>

    suspend fun uploadAsset(
        localMedia: Media,
        targetPath: String? = null,
        checksum: String
    ): Result<CloudMediaEntity> = uploadAsset(localMedia, targetPath)

    suspend fun downloadAsset(remoteId: String): Result<Uri>

    /**
     * Sub-path (relative to `Pictures/`/`Movies/`) where a downloaded copy of
     * [entity] should land. The default mirrors the remote folder layout — correct
     * for providers whose paths are user-visible directories (WebDAV, SMB, NFS).
     * Providers whose remote "path" is internal storage detail (e.g. Immich's
     * asset-store shard tree, which is never a user album) must override this to
     * return a user-meaningful destination such as `<account>/<remote album>`.
     */
    suspend fun downloadSubPath(entity: CloudMediaEntity, accountLabel: String): String =
        entity.relativePath.trim('/').ifBlank { accountLabel }

    /**
     * Fetches remote changes since [timestamp]. When [reconcileIndex] is true the
     * provider should additionally produce a complete remote-id index (if it can do so
     * reliably) so the caller can prune rows whose remote file vanished. Providers with
     * a cheap full scan (WebDAV, SMB, NFS) always report a complete index and may
     * ignore the flag; providers with an expensive index (Immich) honour it on the
     * caller's cadence.
     */
    suspend fun getSyncDelta(timestamp: Long, reconcileIndex: Boolean): Result<SyncDelta>
    suspend fun bulkUploadCheck(hashes: List<String>): Result<Map<String, Boolean>>
    suspend fun verifyRemoteContent(
        localMedia: Media,
        targetPath: String?,
        contentHash: String
    ): Result<Boolean> = bulkUploadCheck(listOf(contentHash)).map { it["0"] == true }

    fun deterministicRemoteId(localMedia: Media, targetPath: String?): String? = null
    fun verifiedRemoteId(contentHash: String): String? = null

    /**
     * Whether [localMedia] is already present at its deterministic upload target.
     * Path-based stores (SMB/NFS/WebDAV) have no server-side content hash, so
     * [bulkUploadCheck] can't dedupe for them; they answer here by checking the
     * target path + size instead. Content-addressable stores (e.g. Immich) keep
     * the default `false` and rely on [bulkUploadCheck].
     */
    suspend fun remoteExists(localMedia: Media, targetPath: String? = null): Boolean = false

    /**
     * Optional hook after a backup worker finishes uploading one or more assets
     * to this provider in a single run. PhotoPrism uses this to trigger import/
     * indexing of the staged upload folder. Default is a no-op.
     */
    suspend fun afterUploadBatch(): Result<Unit> = Result.success(Unit)
}
