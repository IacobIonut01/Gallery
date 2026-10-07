/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a collection with its associated media count and thumbnail.
 * Used for displaying collections in the UI.
 */
data class CollectionWithCount(
    val collection: Collection,
    val mediaCount: Int,
    val thumbnailMediaId: Long?,
    val totalSize: Long = 0
)
