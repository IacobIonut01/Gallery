/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dot.gallery.cloud.core.ProviderType

@Entity(
    tableName = "sync_state",
    indices = [Index(value = ["providerType", "serverConfigId"])]
)
data class SyncStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncCursor: String? = null,
    val totalRemoteAssets: Int = 0,
    val cachedAssets: Int = 0,
    val pendingUploads: Int = 0,
    val lastError: String? = null
)
