/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Entity
import androidx.room3.Index
import com.dot.gallery.cloud.core.ProviderType

/**
 * Junction: one cloud photo ([remoteId]) may belong to many albums without
 * duplicating [CloudMediaEntity] rows.
 */
@Entity(
    tableName = "cloud_album_members",
    primaryKeys = ["albumId", "remoteId", "providerType", "serverConfigId"],
    indices = [
        Index(value = ["albumId", "providerType", "serverConfigId"]),
        Index(value = ["remoteId", "providerType", "serverConfigId"])
    ]
)
data class CloudAlbumMemberEntity(
    /** Remote album id (provider album UID). */
    val albumId: String,
    /** Remote media id (same as [CloudMediaEntity.remoteId]). */
    val remoteId: String,
    val providerType: ProviderType,
    val serverConfigId: Long
)
