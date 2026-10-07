/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL
import com.dot.gallery.data.entity.CloudUploadPrefEntity


/**
 * Migration from version 33 to 34: scope backup album selection per cloud account.
 *
 * Previously `cloud_upload_pref` was keyed only by `albumId`, so album backup
 * selections were global and could not be attributed to a specific cloud
 * account. The table is rebuilt with a composite primary key
 * `(serverConfigId, albumId)` plus a denormalised `providerType` column.
 *
 * Existing (global) selections are migrated onto the most relevant account:
 * the first active sync-enabled config, falling back to the first active
 * config, falling back to the first config. If no cloud account exists the
 * old rows are dropped (they had no destination anyway).
 *
 * The new table definition mirrors exactly what Room generates for
 * [com.dot.gallery.data.entity.CloudUploadPrefEntity] so the post-migration
 * schema validation passes.
 */
val MIGRATION_33_34 = object : Migration(33, 34) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS `cloud_upload_pref_new` (
                `serverConfigId` INTEGER NOT NULL,
                `albumId` INTEGER NOT NULL,
                `providerType` TEXT NOT NULL,
                `albumLabel` TEXT NOT NULL,
                `uploadEnabled` INTEGER NOT NULL,
                `deleteLocalAfterUpload` INTEGER NOT NULL,
                PRIMARY KEY(`serverConfigId`, `albumId`)
            )
            """.trimIndent()
        )

        // Migrate existing global rows onto the best-matching account.
        connection.executeSQL(
            """
            INSERT OR IGNORE INTO `cloud_upload_pref_new`
                (serverConfigId, albumId, providerType, albumLabel, uploadEnabled, deleteLocalAfterUpload)
            SELECT cfg.id, p.albumId, cfg.providerType, p.albumLabel, p.uploadEnabled, p.deleteLocalAfterUpload
            FROM `cloud_upload_pref` p
            CROSS JOIN (
                SELECT id, providerType FROM `cloud_server_config`
                ORDER BY (
                    CASE
                        WHEN isActive = 1 AND syncEnabled = 1 THEN 0
                        WHEN isActive = 1 THEN 1
                        ELSE 2
                    END
                ) ASC, id ASC
                LIMIT 1
            ) cfg
            """.trimIndent()
        )

        connection.executeSQL("DROP TABLE `cloud_upload_pref`")
        connection.executeSQL("ALTER TABLE `cloud_upload_pref_new` RENAME TO `cloud_upload_pref`")
        connection.executeSQL(
            "CREATE INDEX IF NOT EXISTS `index_cloud_upload_pref_serverConfigId` " +
                "ON `cloud_upload_pref` (`serverConfigId`)"
        )
    }
}
