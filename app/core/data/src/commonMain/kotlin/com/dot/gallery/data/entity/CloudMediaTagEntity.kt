/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Entity
import androidx.room3.Index
import com.dot.gallery.cloud.core.ProviderType

/** Join row: remote asset [remoteId] carries tag [tagId] on the given account. */
@Entity(
    tableName = "cloud_media_tag",
    primaryKeys = ["serverConfigId", "providerType", "tagId", "remoteId"],
    indices = [Index(value = ["remoteId", "providerType", "serverConfigId"])]
)
data class CloudMediaTagEntity(
    val serverConfigId: Long,
    val providerType: ProviderType,
    val tagId: String,
    val remoteId: String
)
