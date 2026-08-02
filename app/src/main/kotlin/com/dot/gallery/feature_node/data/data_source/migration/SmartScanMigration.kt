/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_source.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.cloudMediaId

/**
 * Adds durable smart-scan orchestration state and a single account-safe media id namespace.
 *
 * Cloud ids are calculated in Kotlin with the same [cloudMediaId] function used at runtime. SQL
 * cannot reproduce that SHA-256 based derivation. The unique index is deliberately created only
 * after every old row has been backfilled and checked for the (extremely unlikely) hash collision.
 */
val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `cloud_media` ADD COLUMN `globalMediaId` INTEGER NOT NULL DEFAULT 0")
        backfillGlobalMediaIds(db)
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_cloud_media_globalMediaId` " +
                "ON `cloud_media` (`globalMediaId`)"
        )

        db.execSQL("ALTER TABLE `image_embeddings` ADD COLUMN `resultRevision` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `detected_faces` ADD COLUMN `resultRevision` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `ocr_results` ADD COLUMN `resultRevision` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `media_category` ADD COLUMN `resultRevision` TEXT NOT NULL DEFAULT ''")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `smart_scan_runs` (
                `runId` TEXT NOT NULL,
                `trigger` TEXT NOT NULL,
                `requestedFeatures` INTEGER NOT NULL DEFAULT 0,
                `userVisible` INTEGER NOT NULL DEFAULT 0,
                `fullRefresh` INTEGER NOT NULL DEFAULT 0,
                `workId` TEXT,
                `currentPhase` TEXT,
                `status` TEXT NOT NULL DEFAULT 'queued',
                `requestedAt` INTEGER NOT NULL,
                `startedAt` INTEGER,
                `finishedAt` INTEGER,
                `updatedAt` INTEGER NOT NULL,
                `leaseOwner` TEXT,
                `leaseExpiresAt` INTEGER,
                `sourceSnapshot` TEXT NOT NULL DEFAULT '',
                `totalMedia` INTEGER NOT NULL DEFAULT 0,
                `processedMedia` INTEGER NOT NULL DEFAULT 0,
                `succeededMedia` INTEGER NOT NULL DEFAULT 0,
                `skippedMedia` INTEGER NOT NULL DEFAULT 0,
                `failedMedia` INTEGER NOT NULL DEFAULT 0,
                `lastErrorCode` TEXT,
                PRIMARY KEY(`runId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_smart_scan_runs_status` ON `smart_scan_runs` (`status`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_smart_scan_runs_requestedAt` " +
                "ON `smart_scan_runs` (`requestedAt`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_smart_scan_runs_leaseExpiresAt` " +
                "ON `smart_scan_runs` (`leaseExpiresAt`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `smart_scan_phases` (
                `runId` TEXT NOT NULL,
                `phase` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'queued',
                `startedAt` INTEGER,
                `finishedAt` INTEGER,
                `updatedAt` INTEGER NOT NULL,
                `leaseOwner` TEXT,
                `leaseExpiresAt` INTEGER,
                `processorRevision` TEXT NOT NULL DEFAULT '',
                `attemptCount` INTEGER NOT NULL DEFAULT 0,
                `totalMedia` INTEGER NOT NULL DEFAULT 0,
                `processedMedia` INTEGER NOT NULL DEFAULT 0,
                `succeededMedia` INTEGER NOT NULL DEFAULT 0,
                `skippedMedia` INTEGER NOT NULL DEFAULT 0,
                `failedMedia` INTEGER NOT NULL DEFAULT 0,
                `lastErrorCode` TEXT,
                PRIMARY KEY(`runId`, `phase`),
                FOREIGN KEY(`runId`) REFERENCES `smart_scan_runs`(`runId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_smart_scan_phases_runId` ON `smart_scan_phases` (`runId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_smart_scan_phases_status` ON `smart_scan_phases` (`status`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_smart_scan_phases_leaseExpiresAt` " +
                "ON `smart_scan_phases` (`leaseExpiresAt`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `media_feature_state` (
                `mediaId` INTEGER NOT NULL,
                `feature` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'pending',
                `sourceRevision` TEXT NOT NULL DEFAULT '',
                `resultRevision` TEXT NOT NULL DEFAULT '',
                `attemptCount` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL,
                `lastAttemptAt` INTEGER,
                `nextRetryAt` INTEGER,
                `leaseOwner` TEXT,
                `leaseExpiresAt` INTEGER,
                `runId` TEXT,
                `lastErrorCode` TEXT,
                PRIMARY KEY(`mediaId`, `feature`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_feature_state_feature_status` " +
                "ON `media_feature_state` (`feature`, `status`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_feature_state_status` " +
                "ON `media_feature_state` (`status`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_feature_state_leaseExpiresAt` " +
                "ON `media_feature_state` (`leaseExpiresAt`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_media_feature_state_runId` " +
                "ON `media_feature_state` (`runId`)"
        )

        // media_version is shared by the old media and metadata refresh paths. Neither old marker
        // proves that the newly revisioned feature outputs are complete, so force a safe refresh.
        db.execSQL("DELETE FROM `media_version`")
    }
}

private fun backfillGlobalMediaIds(db: SupportSQLiteDatabase) {
    data class CloudKey(val remoteId: String, val providerType: ProviderType, val serverConfigId: Long)

    val ids = HashMap<Long, CloudKey>()
    val rows = mutableListOf<Pair<CloudKey, Long>>()
    db.query(
        "SELECT `remoteId`, `providerType`, `serverConfigId` FROM `cloud_media`"
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val key = CloudKey(
                remoteId = cursor.getString(0),
                providerType = ProviderType.valueOf(cursor.getString(1)),
                serverConfigId = cursor.getLong(2)
            )
            val globalMediaId = cloudMediaId(key.providerType, key.serverConfigId, key.remoteId)
            val previous = ids.putIfAbsent(globalMediaId, key)
            check(previous == null || previous == key) {
                "cloudMediaId collision for $previous and $key (id=$globalMediaId)"
            }
            rows += key to globalMediaId
        }
    }

    rows.forEach { (key, globalMediaId) ->
        db.execSQL(
            """
            UPDATE `cloud_media`
            SET `globalMediaId` = ?
            WHERE `remoteId` = ? AND `providerType` = ? AND `serverConfigId` = ?
            """.trimIndent(),
            arrayOf<Any>(globalMediaId, key.remoteId, key.providerType.name, key.serverConfigId)
        )
    }
}
