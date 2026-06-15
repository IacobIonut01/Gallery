/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_upload_pref")
data class CloudUploadPrefEntity(
    @PrimaryKey
    val albumId: Long,
    val albumLabel: String = "",
    val uploadEnabled: Boolean = false,
    val deleteLocalAfterUpload: Boolean = false
)
