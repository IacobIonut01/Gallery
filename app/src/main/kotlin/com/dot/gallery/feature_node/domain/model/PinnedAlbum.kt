/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_table")
data class PinnedAlbum(
    @PrimaryKey(autoGenerate = false)
    val id: Long
)
