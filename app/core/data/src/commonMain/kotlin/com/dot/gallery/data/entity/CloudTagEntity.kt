/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Entity
import androidx.room3.Index
import com.dot.gallery.cloud.core.ProviderType

/** A tag defined on a cloud server (e.g. Immich tags), synced read-only. */
@Entity(
    tableName = "cloud_tag",
    primaryKeys = ["serverConfigId", "tagId"],
    indices = [Index(value = ["providerType"])]
)
data class CloudTagEntity(
    val serverConfigId: Long,
    val providerType: ProviderType,
    val tagId: String,
    val name: String,
    val value: String,
    val color: String? = null,
    val lastSyncedAt: Long = 0L
)
