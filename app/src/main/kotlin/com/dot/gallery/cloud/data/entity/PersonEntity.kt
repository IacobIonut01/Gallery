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
    tableName = "people",
    indices = [Index(value = ["providerType"])]
)
data class PersonEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val providerType: ProviderType,
    val thumbnailMediaId: Long? = null,
    val thumbnailUrl: String? = null,
    val faceCount: Int = 0,
    val lastUpdated: Long = 0L
)
