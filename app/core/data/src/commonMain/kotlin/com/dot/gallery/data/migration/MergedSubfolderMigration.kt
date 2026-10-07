/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.data.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step
import com.dot.gallery.domain.util.volume


val MIGRATION_41_42 = object : Migration(41, 42) {
    override suspend fun migrate(connection: SQLiteConnection) {
        val hasFullRefresh = connection.prepare("PRAGMA table_info(`smart_scan_runs`)").use { cursor ->
            val nameIndex = 1 /* name column index in PRAGMA table_info */
            var found = false
            while (cursor.step()) {
                if (cursor.getText(nameIndex) == "fullRefresh") {
                    found = true
                    break
                }
            }
            found
        }
        if (!hasFullRefresh) {
            connection.executeSQL("ALTER TABLE `smart_scan_runs` ADD COLUMN `fullRefresh` INTEGER NOT NULL DEFAULT 0")
        }
        connection.executeSQL("ALTER TABLE `merged_subfolder_table` ADD COLUMN `folderKey` TEXT DEFAULT NULL")
        connection.executeSQL("ALTER TABLE `merged_subfolder_table` ADD COLUMN `volume` TEXT NOT NULL DEFAULT ''")
        connection.executeSQL("ALTER TABLE `merged_subfolder_table` ADD COLUMN `relativePath` TEXT NOT NULL DEFAULT ''")
        connection.executeSQL("ALTER TABLE `merged_subfolder_table` ADD COLUMN `displayMode` TEXT NOT NULL DEFAULT 'combined'")
        connection.executeSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_merged_subfolder_table_folderKey` ON `merged_subfolder_table` (`folderKey`)")
    }
}
