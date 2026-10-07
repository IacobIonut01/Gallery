/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.dot.gallery.data.entity.CloudOfflinePinEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface CloudOfflinePinDao {

    @Query("SELECT * FROM cloud_offline_pin ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CloudOfflinePinEntity>>

    @Query("SELECT * FROM cloud_offline_pin")
    suspend fun getAllAsync(): List<CloudOfflinePinEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM cloud_offline_pin WHERE serverConfigId = :configId)")
    fun isPinned(configId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pin: CloudOfflinePinEntity)

    @Query("DELETE FROM cloud_offline_pin WHERE serverConfigId = :configId")
    suspend fun deleteByConfig(configId: Long)
}
