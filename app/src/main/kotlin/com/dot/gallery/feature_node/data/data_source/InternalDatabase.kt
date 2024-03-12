/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_source

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.dot.gallery.feature_node.domain.model.BlacklistedAlbum
import com.dot.gallery.feature_node.domain.model.CustomAlbum
import com.dot.gallery.feature_node.domain.model.CustomAlbumItem
import com.dot.gallery.feature_node.domain.model.PinnedAlbum

@Database(
    entities = [PinnedAlbum::class, BlacklistedAlbum::class, CustomAlbum::class, CustomAlbumItem::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
abstract class InternalDatabase: RoomDatabase() {

    abstract fun getPinnedDao(): PinnedDao

    abstract fun getBlacklistDao(): BlacklistDao

    abstract fun getCustomAlbumDao(): CustomAlbumDao

    companion object {
        const val NAME = "internal_db"
    }
}