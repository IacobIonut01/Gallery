/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.data.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface SyncStateDao {

    @Query("SELECT * FROM sync_state WHERE providerType = :type AND serverConfigId = :configId LIMIT 1")
    suspend fun get(type: ProviderType, configId: Long): SyncStateEntity?

    @Query("SELECT * FROM sync_state")
    fun getAll(): Flow<List<SyncStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)

    @Query("DELETE FROM sync_state WHERE serverConfigId = :configId")
    suspend fun deleteByConfig(configId: Long)
}
