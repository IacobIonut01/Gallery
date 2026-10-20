/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.data.entity.CloudAlbumMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudAlbumMemberDao {

    @Query(
        """
        SELECT remoteId FROM cloud_album_members
        WHERE albumId = :albumId AND providerType = :providerType AND serverConfigId = :serverConfigId
        """
    )
    suspend fun getRemoteIdsForAlbum(
        albumId: String,
        providerType: ProviderType,
        serverConfigId: Long
    ): List<String>

    @Query("SELECT * FROM cloud_album_members")
    fun observeAll(): Flow<List<CloudAlbumMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<CloudAlbumMemberEntity>)

    @Query(
        """
        DELETE FROM cloud_album_members
        WHERE albumId = :albumId AND providerType = :providerType AND serverConfigId = :serverConfigId
        """
    )
    suspend fun deleteAlbum(
        albumId: String,
        providerType: ProviderType,
        serverConfigId: Long
    )

    @Query("DELETE FROM cloud_album_members WHERE serverConfigId = :serverConfigId")
    suspend fun deleteByServer(serverConfigId: Long)

    @Transaction
    suspend fun replaceAlbumMembers(members: List<CloudAlbumMemberEntity>) {
        if (members.isEmpty()) return
        val first = members.first()
        deleteAlbum(first.albumId, first.providerType, first.serverConfigId)
        insertAll(members)
    }
}
