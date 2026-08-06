/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detected_faces",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["personId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class DetectedFaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mediaId: Long,
    val personId: String? = null,
    val embedding: ByteArray? = null,
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val confidence: Float = 0f,
    val timestamp: Long = 0L,
    @ColumnInfo(defaultValue = "''")
    val resultRevision: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetectedFaceEntity) return false
        return id == other.id && mediaId == other.mediaId && personId == other.personId
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + mediaId.hashCode()
        result = 31 * result + (personId?.hashCode() ?: 0)
        return result
    }
}

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
