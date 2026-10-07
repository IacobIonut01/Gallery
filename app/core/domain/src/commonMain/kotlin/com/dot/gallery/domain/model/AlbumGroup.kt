/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "album_groups")
@Immutable
data class AlbumGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val timestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
)
