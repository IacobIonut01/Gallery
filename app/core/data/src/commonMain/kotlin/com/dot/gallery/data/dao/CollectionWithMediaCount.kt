/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.dot.gallery.domain.model.Collection
import kotlinx.coroutines.flow.Flow
import com.dot.gallery.domain.model.CollectionWithCount

data class CollectionWithMediaCount(
    val id: Long,
    val label: String,
    val coverMediaId: Long?,
    val isPinned: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val mediaCount: Int,
    val thumbnailMediaId: Long?,
    val totalSize: Long
) {
    fun toCollection() = Collection(
        id = id,
        label = label,
        coverMediaId = coverMediaId,
        isPinned = isPinned,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun toCollectionWithCount() = com.dot.gallery.domain.model.CollectionWithCount(
        collection = toCollection(),
        mediaCount = mediaCount,
        thumbnailMediaId = thumbnailMediaId,
        totalSize = totalSize
    )
}
