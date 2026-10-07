/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL
import com.dot.gallery.data.entity.CloudDeleteLocalPrefEntity


/**
 * Migration from version 37 to 38: make "delete from device after backup" a GLOBAL per-album
 * setting (new `cloud_delete_local_pref` table) instead of a per-account flag on
 * `cloud_upload_pref`.
 *
 * Rationale: an asset must only be deleted once it's backed up to EVERY cloud its album targets,
 * so the decision can't live per-account. Existing per-account `deleteLocalAfterUpload` flags are
 * collapsed with OR onto the album (if ANY account had it on, the album opts in). The legacy
 * column is left in place (now ignored) to avoid a second table rebuild.
 *
 * The created table mirrors exactly what Room generates for
 * [com.dot.gallery.data.entity.CloudDeleteLocalPrefEntity] so schema validation passes.
 */
val MIGRATION_37_38 = object : Migration(37, 38) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS `cloud_delete_local_pref` (
                `albumId` INTEGER NOT NULL,
                `enabled` INTEGER NOT NULL,
                `albumLabel` TEXT NOT NULL,
                PRIMARY KEY(`albumId`)
            )
            """.trimIndent()
        )
        connection.executeSQL(
            """
            INSERT OR IGNORE INTO `cloud_delete_local_pref` (albumId, enabled, albumLabel)
            SELECT albumId, 1, MAX(albumLabel)
            FROM `cloud_upload_pref`
            WHERE deleteLocalAfterUpload = 1
            GROUP BY albumId
            """.trimIndent()
        )
    }
}
