/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.data.data_source.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val hasFullRefresh = db.query("PRAGMA table_info(`smart_scan_runs`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                .any { it == "fullRefresh" }
        }
        if (!hasFullRefresh) {
            db.execSQL("ALTER TABLE `smart_scan_runs` ADD COLUMN `fullRefresh` INTEGER NOT NULL DEFAULT 0")
        }
        db.execSQL("ALTER TABLE `merged_subfolder_table` ADD COLUMN `folderKey` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `merged_subfolder_table` ADD COLUMN `volume` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `merged_subfolder_table` ADD COLUMN `relativePath` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `merged_subfolder_table` ADD COLUMN `displayMode` TEXT NOT NULL DEFAULT 'combined'")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_merged_subfolder_table_folderKey` ON `merged_subfolder_table` (`folderKey`)")
    }
}
