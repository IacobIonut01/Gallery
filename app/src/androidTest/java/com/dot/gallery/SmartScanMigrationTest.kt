/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.cloudMediaId
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.migration.MIGRATION_40_41
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmartScanMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InternalDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate40To41BackfillsCloudIdentityAndCreatesSmartScanState() {
        helper.createDatabase(TEST_DB, 40).use { db ->
            db.execSQL(
                """
                INSERT INTO cloud_media (
                    remoteId, providerType, serverConfigId, label, path, relativePath,
                    mimeType, timestamp, size, width, height, favorite, trashed, syncState,
                    thumbnailUrl, originalUrl, lastSyncedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    "remote/one.jpg", ProviderType.IMMICH.name, 42L, "one.jpg", "remote/one.jpg", "remote",
                    "image/jpeg", 100L, 200L, 10, 20, 0, 0, "REMOTE_ONLY", "thumb", "original", 100L
                )
            )
            db.execSQL("INSERT INTO media_version (version) VALUES ('old-shared-revision')")
        }

        helper.runMigrationsAndValidate(TEST_DB, 41, true, MIGRATION_40_41).use { db ->
            db.query(
                """
                SELECT globalMediaId FROM cloud_media
                WHERE remoteId = 'remote/one.jpg' AND providerType = 'IMMICH' AND serverConfigId = 42
                """.trimIndent()
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(
                    cloudMediaId(ProviderType.IMMICH, 42L, "remote/one.jpg"),
                    cursor.getLong(0)
                )
            }

            db.query("SELECT COUNT(*) FROM media_version").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            db.query(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'table'
                  AND name IN ('smart_scan_runs', 'smart_scan_phases', 'media_feature_state')
                """.trimIndent()
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(3, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DB = "smart-scan-migration-test"
    }
}
