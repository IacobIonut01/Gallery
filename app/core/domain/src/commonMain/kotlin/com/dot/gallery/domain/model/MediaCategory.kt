/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import kotlinx.serialization.Serializable

/**
 * Junction table linking media items to categories with similarity scores.
 * This allows a media item to belong to multiple categories with different confidence levels.
 *
 * @param mediaId The ID of the media item
 * @param categoryId The ID of the category
 * @param similarityScore The cosine similarity score between the media embedding and category embedding
 * @param addedAt Timestamp when the media was added to this category
 * @param isManuallyAdded Whether the user manually added this media to the category
 */
@Entity(
    tableName = "media_category",
    primaryKeys = ["mediaId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["mediaId"]),
        Index(value = ["similarityScore"])
    ]
)
@Serializable
data class MediaCategory(
    val mediaId: Long,
    val categoryId: Long,
    val similarityScore: Float,
    val addedAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    val isManuallyAdded: Boolean = false,
    @ColumnInfo(defaultValue = "''")
    val resultRevision: String = ""
)
