/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pinned_table")
@Immutable
data class PinnedAlbum(
    @PrimaryKey(autoGenerate = false)
    val id: Long
)
