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
    suspend fun uploadAsset(localMedia: Media, targetPath: String? = null): Result<CloudMediaEntity>
    suspend fun downloadAsset(remoteId: String): Result<Uri>
    suspend fun getChangedSince(timestamp: Long): Result<List<CloudMediaEntity>>
    suspend fun bulkUploadCheck(hashes: List<String>): Result<Map<String, Boolean>>
}
