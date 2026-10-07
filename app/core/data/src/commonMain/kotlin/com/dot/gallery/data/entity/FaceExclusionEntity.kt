/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * "This media does not contain this person" — a user assertion recorded when a photo is
 * removed from a person. Kept separate from `detected_faces` so it survives re-detection
 * (full-refresh scans delete face rows); the face indexers skip clusters excluded for the
 * media being clustered instead of re-adding the face to the same person.
 */
@Entity(
    tableName = "face_exclusions",
    primaryKeys = ["mediaId", "personId"],
    indices = [Index(value = ["personId"])],
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FaceExclusionEntity(
    val mediaId: Long,
    val personId: String,
    val createdAt: Long = 0L
)
