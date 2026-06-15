/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.entity.CloudServerConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudServerConfigDao {

    @Query("SELECT * FROM cloud_server_config ORDER BY id ASC")
    fun getAll(): Flow<List<CloudServerConfigEntity>>

    @Query("SELECT * FROM cloud_server_config WHERE isActive = 1 ORDER BY id ASC")
    fun getActive(): Flow<List<CloudServerConfigEntity>>

    @Query("SELECT * FROM cloud_server_config WHERE providerType = :type ORDER BY id ASC")
    fun getByProvider(type: ProviderType): Flow<List<CloudServerConfigEntity>>

    @Query("SELECT * FROM cloud_server_config WHERE providerType = :type AND isActive = 1 LIMIT 1")
    suspend fun getActiveByProvider(type: ProviderType): CloudServerConfigEntity?

    @Query("SELECT * FROM cloud_server_config WHERE id = :id")
    suspend fun getById(id: Long): CloudServerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: CloudServerConfigEntity): Long

    @Update
    suspend fun update(config: CloudServerConfigEntity)

    @Query("DELETE FROM cloud_server_config WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE cloud_server_config SET isActive = 0 WHERE providerType = :type")
    suspend fun deactivateAllForProvider(type: ProviderType)

    @Query("UPDATE cloud_server_config SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)
}
