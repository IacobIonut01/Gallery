/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dot.gallery.feature_node.domain.model.PinnedAlbum

@Database(
    entities = [PinnedAlbum::class],
    version = 1,
    exportSchema = true
)
abstract class InternalDatabase: RoomDatabase() {

    abstract fun getPinnedDao(): PinnedDao

    companion object {
        const val NAME = "internal_db"
    }
}