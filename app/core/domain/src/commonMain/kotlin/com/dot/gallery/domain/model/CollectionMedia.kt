/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import kotlinx.serialization.Serializable

@Entity(
    tableName = "collection_media",
    primaryKeys = ["collectionId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["mediaId"])
    ]
)
@Immutable
@Serializable
data class CollectionMedia(
    val collectionId: Long,
    val mediaId: Long,
    val addedAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
)
