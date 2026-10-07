/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "collections")
@Immutable
@Serializable
data class Collection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val coverMediaId: Long? = null,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
)
