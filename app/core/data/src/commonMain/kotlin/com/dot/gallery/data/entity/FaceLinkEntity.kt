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
 * A face-scoped user assertion that survives re-detection and batch re-clustering —
 * the durable record behind manual merges ("these faces ARE this person") and
 * face-level / person-pair corrections ("this face is NOT that person").
 *
 * The asserted face is identified by its media + bounding box, matched to live
 * `detected_faces` rows by IoU at apply time (face ids regenerate on re-detection,
 * so a foreign key to them would be useless).
 */
@Entity(
    tableName = "face_links",
    indices = [
        Index(value = ["personId"]),
        Index(value = ["mediaId", "left", "top", "right", "bottom", "personId", "kind"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FaceLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mediaId: Long,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val personId: String,
    val kind: FaceLinkKind,
    val createdAt: Long = 0L
)
