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
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudMediaDao {

    @Query("SELECT * FROM cloud_media WHERE providerType = :providerType ORDER BY timestamp DESC")
    fun getByProvider(providerType: ProviderType): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE serverConfigId = :configId ORDER BY timestamp DESC")
    fun getByServerConfig(configId: Long): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE favorite = 1 AND trashed = 0 AND archived = 0 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE favorite = 1 AND providerType = :providerType ORDER BY timestamp DESC")
    fun getFavoritesByProvider(providerType: ProviderType): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE trashed = 1 ORDER BY timestamp DESC")
    fun getTrashed(): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE trashed = 1 AND providerType = :providerType ORDER BY timestamp DESC")
    fun getTrashedByProvider(providerType: ProviderType): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE remoteId = :remoteId AND providerType = :providerType")
    suspend fun getByRemoteId(remoteId: String, providerType: ProviderType): CloudMediaEntity?

    @Query("SELECT * FROM cloud_media WHERE contentHash = :hash LIMIT 1")
    suspend fun getByContentHash(hash: String): CloudMediaEntity?

    @Query("SELECT * FROM cloud_media WHERE syncState = :state ORDER BY timestamp DESC")
    fun getBySyncState(state: SyncState): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getAll(limit: Int, offset: Int): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE trashed = 0 AND archived = 0 ORDER BY timestamp DESC")
    fun getAllForTimeline(): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE trashed = 0 AND archived = 0 ORDER BY timestamp DESC")
    suspend fun getAllCachedAsync(): List<CloudMediaEntity>

    @Query("SELECT * FROM cloud_media ORDER BY timestamp DESC")
    suspend fun getAllAsync(): List<CloudMediaEntity>

    @Query("SELECT * FROM cloud_media WHERE archived = 1 AND trashed = 0 ORDER BY timestamp DESC")
    suspend fun getArchivedAsync(): List<CloudMediaEntity>

    @Query("SELECT * FROM cloud_media WHERE archived = 1 AND trashed = 0 AND providerType = :providerType ORDER BY timestamp DESC")
    suspend fun getArchivedByProviderAsync(providerType: ProviderType): List<CloudMediaEntity>

    @Query("SELECT * FROM cloud_media WHERE favorite = 1 AND trashed = 0 AND archived = 0 ORDER BY timestamp DESC")
    suspend fun getFavoritesAsync(): List<CloudMediaEntity>

    @Query("SELECT * FROM cloud_media WHERE trashed = 1 ORDER BY timestamp DESC")
    suspend fun getTrashedAsync(): List<CloudMediaEntity>

    @Query("SELECT COUNT(*) FROM cloud_media WHERE providerType = :providerType")
    suspend fun countByProvider(providerType: ProviderType): Int

    @Query("SELECT COUNT(*) FROM cloud_media WHERE trashed = 0 AND archived = 0")
    suspend fun countCached(): Int

    @Query("SELECT COUNT(*) FROM cloud_media WHERE favorite = 1 AND trashed = 0 AND archived = 0")
    suspend fun countFavorites(): Int

    @Query("SELECT COUNT(*) FROM cloud_media WHERE archived = 1 AND trashed = 0")
    suspend fun countArchived(): Int

    @Query("SELECT COUNT(*) FROM cloud_media WHERE trashed = 1")
    suspend fun countTrashed(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CloudMediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CloudMediaEntity)

    @Update
    suspend fun update(item: CloudMediaEntity)

    @Query("UPDATE cloud_media SET syncState = :state WHERE remoteId = :remoteId AND providerType = :providerType")
    suspend fun updateSyncState(remoteId: String, providerType: ProviderType, state: SyncState)

    @Query("UPDATE cloud_media SET favorite = :favorite WHERE remoteId = :remoteId AND providerType = :providerType")
    suspend fun updateFavorite(remoteId: String, providerType: ProviderType, favorite: Boolean)

    @Query("UPDATE cloud_media SET trashed = :trashed WHERE remoteId = :remoteId AND providerType = :providerType")
    suspend fun updateTrashed(remoteId: String, providerType: ProviderType, trashed: Boolean)

    @Query("UPDATE cloud_media SET archived = :archived WHERE remoteId = :remoteId AND providerType = :providerType")
    suspend fun updateArchived(remoteId: String, providerType: ProviderType, archived: Boolean)

    @Query("UPDATE cloud_media SET localCopyPath = :path, syncState = :state WHERE remoteId = :remoteId AND providerType = :providerType")
    suspend fun updateLocalCopy(remoteId: String, providerType: ProviderType, path: String, state: SyncState)

    @Query("DELETE FROM cloud_media WHERE remoteId = :remoteId AND providerType = :providerType")
    suspend fun delete(remoteId: String, providerType: ProviderType)

    @Query("DELETE FROM cloud_media WHERE serverConfigId = :configId")
    suspend fun deleteByServerConfig(configId: Long)

    @Query("DELETE FROM cloud_media WHERE providerType = :providerType")
    suspend fun deleteByProvider(providerType: ProviderType)

    @Query("DELETE FROM cloud_media")
    suspend fun deleteAll()
}
