/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import android.net.Uri
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.feature_node.domain.model.Media

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
    suspend fun getChangedSince(timestamp: Long): Result<List<CloudMediaEntity>>
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
}
