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

@Entity(
    tableName = "face_clusters",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FaceClusterEntity(
    @PrimaryKey
    val personId: String,
    val centroid: FloatArray,
    val faceCount: Int,
    val updatedAt: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceClusterEntity) return false
        return personId == other.personId && centroid.contentEquals(other.centroid) &&
            faceCount == other.faceCount && updatedAt == other.updatedAt
    }

    override fun hashCode(): Int {
        var result = personId.hashCode()
        result = 31 * result + centroid.contentHashCode()
        result = 31 * result + faceCount
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
