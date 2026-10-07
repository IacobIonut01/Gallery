/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Entity
import androidx.room3.Index
import com.dot.gallery.cloud.core.ProviderType

@Entity(
    tableName = "cloud_backup_revision",
    primaryKeys = ["serverConfigId", "localUri"],
    indices = [
        Index(value = ["serverConfigId"]),
        Index(value = ["providerType"]),
        Index(value = ["serverConfigId", "remoteId"])
    ]
)
data class CloudBackupRevisionEntity(
    val serverConfigId: Long,
    val providerType: ProviderType,
    val localUri: String,
    val localSize: Long,
    val localTimestamp: Long,
    val remoteId: String,
    val remoteFingerprint: String,
    val verifiedAt: Long
)
