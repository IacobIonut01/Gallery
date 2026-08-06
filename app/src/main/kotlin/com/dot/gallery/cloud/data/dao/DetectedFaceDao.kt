/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.dot.gallery.cloud.data.entity.DetectedFaceEntity
import com.dot.gallery.cloud.data.entity.FaceClusterEntity
import kotlinx.coroutines.flow.Flow

data class DetectedFaceHeader(
    val mediaId: Long,
    val timestamp: Long,
    val resultRevision: String
)

data class DetectedFacePersonCount(
    val personId: String,
    val faceCount: Int
)

@Dao
interface DetectedFaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(face: DetectedFaceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(faces: List<DetectedFaceEntity>)

    @Query("SELECT * FROM detected_faces WHERE personId = :personId ORDER BY confidence DESC")
    fun getByPerson(personId: String): Flow<List<DetectedFaceEntity>>

    @Query("SELECT DISTINCT mediaId FROM detected_faces WHERE personId = :personId")
    suspend fun getMediaIdsForPerson(personId: String): List<Long>

    @Query("SELECT * FROM detected_faces WHERE mediaId = :mediaId")
    suspend fun getByMedia(mediaId: Long): List<DetectedFaceEntity>

    @Query("SELECT * FROM detected_faces")
    suspend fun getAll(): List<DetectedFaceEntity>

    @Query("SELECT mediaId, timestamp, resultRevision FROM detected_faces")
    suspend fun getHeaders(): List<DetectedFaceHeader>

    @Query("SELECT DISTINCT mediaId FROM detected_faces")
    suspend fun getIndexedMediaIds(): List<Long>

    @Query("SELECT * FROM face_clusters")
    suspend fun getClusters(): List<FaceClusterEntity>

    @Query("SELECT personId, COUNT(*) AS faceCount FROM detected_faces WHERE personId IS NOT NULL GROUP BY personId")
    suspend fun getPersonCounts(): List<DetectedFacePersonCount>

    @Upsert
    suspend fun upsertClusters(clusters: List<FaceClusterEntity>)

    @Query("DELETE FROM face_clusters")
    suspend fun deleteClusters()

    @Query("DELETE FROM face_clusters WHERE personId IN (:personIds)")
    suspend fun deleteClusters(personIds: List<String>)

    @Transaction
    suspend fun replaceClusters(clusters: List<FaceClusterEntity>) {
        deleteClusters()
        if (clusters.isNotEmpty()) upsertClusters(clusters)
    }

    @Query("UPDATE detected_faces SET resultRevision = :revision WHERE mediaId = :mediaId")
    suspend fun updateResultRevision(mediaId: Long, revision: String): Int

    @Query("SELECT COUNT(*) FROM detected_faces WHERE personId = :personId")
    suspend fun countForPerson(personId: String): Int

    @Query("UPDATE detected_faces SET personId = :newPersonId WHERE personId = :oldPersonId")
    suspend fun reassignPerson(oldPersonId: String, newPersonId: String)

    @Query("UPDATE detected_faces SET personId = :personId WHERE id = :faceId")
    suspend fun assignFace(faceId: Long, personId: String?)

    @Query("DELETE FROM detected_faces WHERE mediaId = :mediaId")
    suspend fun deleteByMedia(mediaId: Long)

    @Query(
        """
        SELECT DISTINCT personId FROM detected_faces
        WHERE personId IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM media WHERE media.id = detected_faces.mediaId)
          AND NOT EXISTS (SELECT 1 FROM cloud_media WHERE cloud_media.globalMediaId = detected_faces.mediaId)
        """
    )
    suspend fun getOrphanPersonIds(): List<String>

    @Query(
        """
        DELETE FROM detected_faces
        WHERE NOT EXISTS (SELECT 1 FROM media WHERE media.id = detected_faces.mediaId)
          AND NOT EXISTS (SELECT 1 FROM cloud_media WHERE cloud_media.globalMediaId = detected_faces.mediaId)
        """
    )
    suspend fun deleteOrphans(): Int

    @Query("DELETE FROM detected_faces")
    suspend fun deleteAll()
}
