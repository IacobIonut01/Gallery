/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL


/**
 * Scopes cloud album sync preferences to an individual cloud account.
 *
 * The old primary key `(albumRemoteId, providerType)` allowed two accounts of the same
 * provider type to overwrite each other's preference when their servers reused an album id.
 * Existing rows already contain `serverConfigId`, so rebuilding the table preserves each
 * surviving row and extends the primary key with that account id.
 */
val MIGRATION_43_44 = object : Migration(43, 44) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
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
        connection.executeSQL(
            """
            INSERT OR IGNORE INTO `cloud_album_sync_new`
                (`albumRemoteId`, `providerType`, `serverConfigId`, `albumName`, `syncEnabled`)
            SELECT `albumRemoteId`, `providerType`, `serverConfigId`, `albumName`, `syncEnabled`
            FROM `cloud_album_sync`
            """.trimIndent()
        )
        connection.executeSQL("DROP TABLE `cloud_album_sync`")
        connection.executeSQL("ALTER TABLE `cloud_album_sync_new` RENAME TO `cloud_album_sync`")
    }
}

val MIGRATION_44_45 = object : Migration(44, 45) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS `cloud_backup_revision` (
                `serverConfigId` INTEGER NOT NULL,
                `providerType` TEXT NOT NULL,
                `localUri` TEXT NOT NULL,
                `localSize` INTEGER NOT NULL,
                `localTimestamp` INTEGER NOT NULL,
                `remoteId` TEXT NOT NULL,
                `remoteFingerprint` TEXT NOT NULL,
                `verifiedAt` INTEGER NOT NULL,
                PRIMARY KEY(`serverConfigId`, `localUri`)
            )
            """.trimIndent()
        )
        connection.executeSQL(
            """
            INSERT OR REPLACE INTO `cloud_backup_revision` (
                `serverConfigId`, `providerType`, `localUri`, `localSize`, `localTimestamp`,
                `remoteId`, `remoteFingerprint`, `verifiedAt`
            )
            SELECT `serverConfigId`, `providerType`, `localCopyPath`, `size`, `timestamp`,
                `remoteId`, `contentHash`, CAST(strftime('%s', 'now') AS INTEGER) * 1000
            FROM `cloud_media`
            WHERE `localCopyPath` != '' AND `contentHash` IS NOT NULL AND `contentHash` != ''
            """.trimIndent()
        )
        connection.executeSQL(
            "CREATE INDEX IF NOT EXISTS `index_cloud_backup_revision_serverConfigId` " +
                "ON `cloud_backup_revision` (`serverConfigId`)"
        )
        connection.executeSQL(
            "CREATE INDEX IF NOT EXISTS `index_cloud_backup_revision_providerType` " +
                "ON `cloud_backup_revision` (`providerType`)"
        )
        connection.executeSQL(
            "CREATE INDEX IF NOT EXISTS `index_cloud_backup_revision_serverConfigId_remoteId` " +
                "ON `cloud_backup_revision` (`serverConfigId`, `remoteId`)"
        )
    }
}
