/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL


val MIGRATION_45_46 = object : Migration(45, 46) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            "ALTER TABLE `media` ADD COLUMN `captureTimeOrigin` TEXT NOT NULL " +
                "DEFAULT 'modified_fallback'"
        )
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS `media_capture_time` (
                `mediaId` INTEGER NOT NULL,
                `captureTimestampMillis` INTEGER NOT NULL,
                `origin` TEXT NOT NULL,
                `sourceModifiedSeconds` INTEGER NOT NULL,
                `sourceTakenTimestampMillis` INTEGER,
                `sourceSize` INTEGER NOT NULL,
                `sourcePath` TEXT NOT NULL,
                `updatedAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`mediaId`)
            )
            """.trimIndent()
        )
        connection.executeSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_capture_time_origin` " +
                "ON `media_capture_time` (`origin`)"
        )
    }
}
