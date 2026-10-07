/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.domain.uri.path


/**
 * Per-account backup album selection.
 *
 * Backup is scoped to a specific cloud account ([serverConfigId]) so that each
 * configured cloud independently chooses which local albums it backs up. The
 * same album can therefore be selected for one account and not another.
 * [providerType] is denormalised from the owning server config for cheap
 * grouping/labelling in the UI and worker.
 */
@Entity(
    tableName = "cloud_upload_pref",
    primaryKeys = ["serverConfigId", "albumId"],
    indices = [Index(value = ["serverConfigId"])]
)
data class CloudUploadPrefEntity(
    val serverConfigId: Long,
    val albumId: Long,
    val providerType: ProviderType,
    val albumLabel: String = "",
    val uploadEnabled: Boolean = false,
    val deleteLocalAfterUpload: Boolean = false,
    /**
     * Optional per-album remote folder override for path-based providers
     * (WebDAV/ownCloud/Nextcloud/SMB/NFS). When non-blank it is used verbatim
     * instead of "<account base>/<albumLabel>". Blank = derive from the album
     * label under the account's upload base path.
     */
    @ColumnInfo(defaultValue = "") val customPath: String = ""
)
