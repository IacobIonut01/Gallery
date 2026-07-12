/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dot.gallery.cloud.data.entity.DetectedFaceEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT DISTINCT mediaId FROM detected_faces")
    suspend fun getIndexedMediaIds(): List<Long>

    @Query("SELECT COUNT(*) FROM detected_faces WHERE personId = :personId")
    suspend fun countForPerson(personId: String): Int

    @Query("UPDATE detected_faces SET personId = :newPersonId WHERE personId = :oldPersonId")
    suspend fun reassignPerson(oldPersonId: String, newPersonId: String)

    @Query("UPDATE detected_faces SET personId = :personId WHERE id = :faceId")
    suspend fun assignFace(faceId: Long, personId: String?)

    @Query("DELETE FROM detected_faces WHERE mediaId = :mediaId")
    suspend fun deleteByMedia(mediaId: Long)

    @Query("DELETE FROM detected_faces")
    suspend fun deleteAll()
}
