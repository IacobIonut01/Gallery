/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL


val MIGRATION_46_47 = object : Migration(46, 47) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS `cloud_tag` (
                `serverConfigId` INTEGER NOT NULL,
                `providerType` TEXT NOT NULL,
                `tagId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `value` TEXT NOT NULL,
                `color` TEXT,
                `lastSyncedAt` INTEGER NOT NULL,
                PRIMARY KEY(`serverConfigId`, `tagId`)
            )
            """.trimIndent()
        )
        connection.executeSQL(
            "CREATE INDEX IF NOT EXISTS `index_cloud_tag_providerType` " +
                "ON `cloud_tag` (`providerType`)"
        )
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS `cloud_media_tag` (
                `serverConfigId` INTEGER NOT NULL,
                `providerType` TEXT NOT NULL,
                `tagId` TEXT NOT NULL,
                `remoteId` TEXT NOT NULL,
                PRIMARY KEY(`serverConfigId`, `providerType`, `tagId`, `remoteId`)
            )
            """.trimIndent()
        )
        connection.executeSQL(
            "CREATE INDEX IF NOT EXISTS `index_cloud_media_tag_remoteId_providerType_serverConfigId` " +
                "ON `cloud_media_tag` (`remoteId`, `providerType`, `serverConfigId`)"
        )
    }
}
