/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_source.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Scopes cloud album sync preferences to an individual cloud account.
 *
 * The old primary key `(albumRemoteId, providerType)` allowed two accounts of the same
 * provider type to overwrite each other's preference when their servers reused an album id.
 * Existing rows already contain `serverConfigId`, so rebuilding the table preserves each
 * surviving row and extends the primary key with that account id.
 */
val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cloud_album_sync_new` (
                `albumRemoteId` TEXT NOT NULL,
                `providerType` TEXT NOT NULL,
                `serverConfigId` INTEGER NOT NULL,
                `albumName` TEXT NOT NULL,
                `syncEnabled` INTEGER NOT NULL,
                PRIMARY KEY(`albumRemoteId`, `providerType`, `serverConfigId`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO `cloud_album_sync_new`
                (`albumRemoteId`, `providerType`, `serverConfigId`, `albumName`, `syncEnabled`)
            SELECT `albumRemoteId`, `providerType`, `serverConfigId`, `albumName`, `syncEnabled`
            FROM `cloud_album_sync`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `cloud_album_sync`")
        db.execSQL("ALTER TABLE `cloud_album_sync_new` RENAME TO `cloud_album_sync`")
    }
}
