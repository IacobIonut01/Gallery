/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dot.gallery.cloud.data.entity.CloudUploadPrefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudUploadPrefDao {

    @Query("SELECT * FROM cloud_upload_pref")
    fun getAll(): Flow<List<CloudUploadPrefEntity>>

    @Query("SELECT * FROM cloud_upload_pref WHERE uploadEnabled = 1")
    fun getEnabled(): Flow<List<CloudUploadPrefEntity>>

    @Query("SELECT * FROM cloud_upload_pref WHERE uploadEnabled = 1")
    suspend fun getEnabledList(): List<CloudUploadPrefEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CloudUploadPrefEntity)

    @Query("DELETE FROM cloud_upload_pref WHERE albumId = :albumId")
    suspend fun delete(albumId: Long)
}
