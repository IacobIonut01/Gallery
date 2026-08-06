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
import androidx.room.Update
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import kotlinx.coroutines.flow.Flow

data class CloudMediaLocalState(
    val remoteId: String,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val localCopyPath: String,
    val size: Long,
    val timestamp: Long,
    val syncState: SyncState
)

data class CloudMediaLocalRevision(
    val localCopyPath: String,
    val size: Long,
    val timestamp: Long
)

data class CloudMediaRemoteRevision(
    val fileId: String,
    val label: String,
    val mimeType: String,
    val size: Long,
    val lastSyncedAt: Long
)

@Dao
interface CloudMediaDao {

    @Query("SELECT * FROM cloud_media WHERE providerType = :providerType ORDER BY timestamp DESC")
    fun getByProvider(providerType: ProviderType): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE serverConfigId = :configId ORDER BY timestamp DESC")
    fun getByServerConfig(configId: Long): Flow<List<CloudMediaEntity>>

    @Query(
        """
        SELECT localCopyPath, size, timestamp FROM cloud_media
        WHERE serverConfigId = :configId AND localCopyPath != ''
            AND syncState = 'SYNCED' AND trashed = 0
        """
    )
    suspend fun getLocalRevisions(configId: Long): List<CloudMediaLocalRevision>

    @Query(
        """
        SELECT fileId, label, mimeType, size, lastSyncedAt FROM cloud_media
        WHERE serverConfigId = :configId AND fileId != '' AND lastSyncedAt > 0
            AND trashed = 0
        """
    )
    suspend fun getRemoteRevisions(configId: Long): List<CloudMediaRemoteRevision>

    @Query(
        """
        SELECT remoteId, providerType, serverConfigId, localCopyPath, size, timestamp, syncState FROM cloud_media
        WHERE serverConfigId = :configId AND localCopyPath != '' AND remoteId IN (:remoteIds)
        """
    )
    suspend fun getLocalStates(configId: Long, remoteIds: List<String>): List<CloudMediaLocalState>

    @Query("SELECT * FROM cloud_media WHERE favorite = 1 AND trashed = 0 AND archived = 0 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE favorite = 1 AND providerType = :providerType ORDER BY timestamp DESC")
    fun getFavoritesByProvider(providerType: ProviderType): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE trashed = 1 ORDER BY timestamp DESC")
    fun getTrashed(): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE trashed = 1 AND providerType = :providerType ORDER BY timestamp DESC")
    fun getTrashedByProvider(providerType: ProviderType): Flow<List<CloudMediaEntity>>

    @Query("SELECT * FROM cloud_media WHERE globalMediaId = :globalMediaId LIMIT 1")
    suspend fun getByGlobalMediaId(globalMediaId: Long): CloudMediaEntity?

    @Query(
        """
        SELECT * FROM cloud_media
        WHERE remoteId = :remoteId AND providerType = :providerType AND serverConfigId = :serverConfigId
        LIMIT 1
        """
    )
    suspend fun getByRemoteId(
        remoteId: String,
        providerType: ProviderType,
        serverConfigId: Long
    ): CloudMediaEntity?

    @Query("SELECT * FROM cloud_media WHERE contentHash = :hash AND serverConfigId = :serverConfigId LIMIT 1")
    suspend fun getByContentHash(hash: String, serverConfigId: Long): CloudMediaEntity?

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

    @Query("SELECT COUNT(*) FROM cloud_media WHERE serverConfigId = :configId")
    suspend fun countByConfig(configId: Long): Int

    @Query("SELECT COUNT(*) FROM cloud_media WHERE trashed = 0 AND archived = 0")
    suspend fun countCached(): Int

    @Query("SELECT COUNT(*) FROM cloud_media WHERE favorite = 1 AND trashed = 0 AND archived = 0")
    suspend fun countFavorites(): Int

    @Query("SELECT COUNT(*) FROM cloud_media WHERE archived = 1 AND trashed = 0")
    suspend fun countArchived(): Int

    @Query("SELECT COUNT(*) FROM cloud_media WHERE trashed = 1")
    suspend fun countTrashed(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRaw(items: List<CloudMediaEntity>)

    @Transaction
    suspend fun insertAll(items: List<CloudMediaEntity>) {
        items.groupBy { it.serverConfigId }.forEach { (configId, configItems) ->
            configItems.chunked(900).forEach { chunk ->
                val localStates = getLocalStates(configId, chunk.map { it.remoteId })
                    .associateBy { it.providerType to it.remoteId }
                insertAllRaw(
                    chunk.map { item ->
                        val local = localStates[item.providerType to item.remoteId]
                        if (local == null) item else item.copy(
                            localCopyPath = local.localCopyPath,
                            size = local.size,
                            timestamp = local.timestamp,
                            syncState = local.syncState
                        )
                    }
                )
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CloudMediaEntity)

    @Update
    suspend fun update(item: CloudMediaEntity)

    @Query(
        """
        UPDATE cloud_media SET syncState = :state
        WHERE remoteId = :remoteId AND providerType = :providerType AND serverConfigId = :serverConfigId
        """
    )
    suspend fun updateSyncState(
        remoteId: String,
        providerType: ProviderType,
        serverConfigId: Long,
        state: SyncState
    )

    @Query(
        """
        UPDATE cloud_media SET favorite = :favorite
        WHERE remoteId = :remoteId AND providerType = :providerType AND serverConfigId = :serverConfigId
        """
    )
    suspend fun updateFavorite(
        remoteId: String,
        providerType: ProviderType,
        serverConfigId: Long,
        favorite: Boolean
    )

    @Query(
        """
        UPDATE cloud_media SET trashed = :trashed
        WHERE remoteId = :remoteId AND providerType = :providerType AND serverConfigId = :serverConfigId
        """
    )
    suspend fun updateTrashed(
        remoteId: String,
        providerType: ProviderType,
        serverConfigId: Long,
        trashed: Boolean
    )

    @Query(
        """
        UPDATE cloud_media SET archived = :archived
        WHERE remoteId = :remoteId AND providerType = :providerType AND serverConfigId = :serverConfigId
        """
    )
    suspend fun updateArchived(
        remoteId: String,
        providerType: ProviderType,
        serverConfigId: Long,
        archived: Boolean
    )

    @Query(
        """
        UPDATE cloud_media SET localCopyPath = :path, syncState = :state
        WHERE remoteId = :remoteId AND providerType = :providerType AND serverConfigId = :serverConfigId
        """
    )
    suspend fun updateLocalCopy(
        remoteId: String,
        providerType: ProviderType,
        serverConfigId: Long,
        path: String,
        state: SyncState
    )

    @Query(
        """
        DELETE FROM cloud_media
        WHERE remoteId = :remoteId AND providerType = :providerType AND serverConfigId = :serverConfigId
        """
    )
    suspend fun delete(remoteId: String, providerType: ProviderType, serverConfigId: Long)

    @Query("DELETE FROM cloud_media WHERE serverConfigId = :configId")
    suspend fun deleteByServerConfig(configId: Long)

    @Query("DELETE FROM cloud_media WHERE providerType = :providerType")
    suspend fun deleteByProvider(providerType: ProviderType)

    @Query("DELETE FROM cloud_media")
    suspend fun deleteAll()
}
