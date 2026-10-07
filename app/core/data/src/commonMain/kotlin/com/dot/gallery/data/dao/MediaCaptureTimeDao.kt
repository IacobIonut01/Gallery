/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import com.dot.gallery.data.entity.MediaCaptureTimeEntity
import androidx.room3.Entity
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaCaptureTimeDao {
    @Query("SELECT * FROM media_capture_time")
    fun observeAll(): Flow<List<MediaCaptureTimeEntity>>

    @Query("SELECT * FROM media_capture_time")
    suspend fun getAll(): List<MediaCaptureTimeEntity>

    @Upsert
    suspend fun upsertAll(entries: List<MediaCaptureTimeEntity>)

    @Query("DELETE FROM media_capture_time WHERE mediaId IN (:mediaIds)")
    suspend fun deleteByIds(mediaIds: List<Long>)

    @Query("DELETE FROM media_capture_time")
    suspend fun deleteAll()
}
