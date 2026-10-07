/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.dot.gallery.cloud.core.ProviderType


/**
 * An account marked "Available offline". The [com.dot.gallery.cloud.sync.CloudOfflineDownloadWorker]
 * proactively downloads every cached asset of [serverConfigId] into the pinned cache tier and keeps
 * it there (exempt from LRU eviction) until the pin is removed.
 */
@Entity(
    tableName = "cloud_offline_pin",
    indices = [Index(value = ["serverConfigId"], unique = true)]
)
data class CloudOfflinePinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val serverConfigId: Long,
    val providerType: ProviderType,
    val label: String = "",
    val createdAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
)
