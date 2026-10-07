/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.dot.gallery.domain.model.ScannedMedia


@Dao
interface ScannedMediaDao {

    @Query("SELECT id FROM scanned_media")
    suspend fun getScannedIds(): List<Long>

    @Upsert
    suspend fun insertAll(items: List<ScannedMedia>)

    @Query("DELETE FROM scanned_media WHERE id NOT IN (SELECT id FROM media)")
    suspend fun removeStaleEntries()

}
