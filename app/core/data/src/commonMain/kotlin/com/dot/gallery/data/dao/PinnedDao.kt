/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert
import com.dot.gallery.domain.model.PinnedAlbum
import kotlinx.coroutines.flow.Flow


@Dao
interface PinnedDao {

    @Query("SELECT * FROM pinned_table")
    fun getPinnedAlbums(): Flow<List<PinnedAlbum>>

    @Upsert
    suspend fun insertPinnedAlbum(pinnedAlbum: PinnedAlbum)

    @Upsert
    suspend fun insertPinnedAlbums(pinnedAlbums: List<PinnedAlbum>)

    @Delete
    suspend fun removePinnedAlbum(pinnedAlbum: PinnedAlbum)

    @Query("DELETE FROM pinned_table WHERE id IN (:albumIds)")
    suspend fun removePinnedAlbums(albumIds: List<Long>)

    @Query("SELECT EXISTS(SELECT * FROM pinned_table WHERE id = :albumId)")
    suspend fun albumIsPinned(albumId: Long): Boolean

    @Query("SELECT id FROM pinned_table")
    suspend fun getPinnedAlbumIds(): List<Long>

}