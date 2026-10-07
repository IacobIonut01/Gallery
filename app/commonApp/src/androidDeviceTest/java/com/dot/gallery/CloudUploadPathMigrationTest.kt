/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.data.InternalDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Covers the v48 -> v49 AutoMigration that adds per-account upload destinations
 * (`cloud_server_config.uploadBasePath` / `uploadVideosPath`) and the per-album
 * override (`cloud_upload_pref.customPath`). Existing rows must keep their data
 * and receive "" defaults so the resolver falls back to prior behaviour.
 */
@RunWith(AndroidJUnit4::class)
class CloudUploadPathMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        File.createTempFile(
            "cloud-upload-path-migration-test",
            ".db",
            instrumentation.targetContext.cacheDir
        ),
        BundledSQLiteDriver(),
        InternalDatabase::class
    )

    @Test
    fun migrate48To49AddsUploadPathColumnsWithEmptyDefaults() = runBlocking {
        helper.createDatabase(48).use { database ->
            database.execSQL(
                """
                INSERT INTO cloud_server_config
                    (id, providerType, serverUrl, displayName, isActive, lastConnected,
                     syncEnabled, wifiOnly, syncIntervalMinutes, syncFolders)
                VALUES (7, 'WEBDAV', 'https://dav.example', 'NAS', 1, 0, 1, 1, 360, '')
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO cloud_upload_pref
                    (serverConfigId, albumId, providerType, albumLabel, uploadEnabled,
                     deleteLocalAfterUpload)
                VALUES (7, 10, 'WEBDAV', 'Camera', 1, 0)
                """.trimIndent()
            )
        }

        // The v48 -> v49 AutoMigration is discovered from the database class itself.
        helper.runMigrationsAndValidate(49).use { database ->
            database.prepare(
                """
                SELECT displayName, uploadBasePath, uploadVideosPath
                FROM cloud_server_config WHERE id = 7
                """.trimIndent()
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("NAS", statement.getText(0))
                assertEquals("", statement.getText(1))
                assertEquals("", statement.getText(2))
                assertEquals(false, statement.step())
            }
            database.prepare(
                """
                SELECT albumLabel, customPath
                FROM cloud_upload_pref WHERE serverConfigId = 7 AND albumId = 10
                """.trimIndent()
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("Camera", statement.getText(0))
                assertEquals("", statement.getText(1))
                assertEquals(false, statement.step())
            }
        }
    }
}
