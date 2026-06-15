/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.entity

import androidx.room.Entity
import com.dot.gallery.cloud.core.ProviderType

@Entity(
    tableName = "cloud_album_sync",
    primaryKeys = ["albumRemoteId", "providerType"]
)
data class CloudAlbumSyncEntity(
    val albumRemoteId: String,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val albumName: String = "",
    val syncEnabled: Boolean = true
)
