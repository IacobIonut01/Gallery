/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.MergedSubfolderAlbum
import kotlinx.coroutines.flow.Flow

@Dao
interface MergedSubfolderDao {

    @Query("SELECT * FROM merged_subfolder_table")
    fun getMergedSubfolderAlbums(): Flow<List<MergedSubfolderAlbum>>

    @Upsert
    suspend fun insertMergedSubfolderAlbum(mergedSubfolderAlbum: MergedSubfolderAlbum)

    @Delete
    suspend fun removeMergedSubfolderAlbum(mergedSubfolderAlbum: MergedSubfolderAlbum)

    @Query("SELECT id FROM merged_subfolder_table")
    suspend fun getMergedSubfolderAlbumIds(): List<Long>

    @Query("UPDATE merged_subfolder_table SET displayMode = :displayMode WHERE id = :id")
    suspend fun updateDisplayMode(id: Long, displayMode: String)

}
