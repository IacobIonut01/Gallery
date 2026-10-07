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
import com.dot.gallery.data.migration.MIGRATION_45_46
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CaptureTimeMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        File.createTempFile(
            "capture-time-migration-test",
            ".db",
            instrumentation.targetContext.cacheDir
        ),
        BundledSQLiteDriver(),
        InternalDatabase::class
    )

    @Test
    fun migrate45To46AddsCaptureOriginAndIndexTable() = runBlocking {
        helper.createDatabase(45).close()

        helper.runMigrationsAndValidate(46, listOf(MIGRATION_45_46)).use { database ->
            database.prepare("PRAGMA table_info(`media`)").use { statement ->
                val nameIndex = statement.getColumnNames().indexOf("name")
                val columns = buildSet {
                    while (statement.step()) add(statement.getText(nameIndex))
                }
                assertTrue("captureTimeOrigin" in columns)
            }

            database.execSQL(
                """
                INSERT INTO media_capture_time (
                    mediaId, captureTimestampMillis, origin, sourceModifiedSeconds,
                    sourceTakenTimestampMillis, sourceSize, sourcePath, updatedAtMillis
                ) VALUES (7, 1000, 'embedded_image', 2, NULL, 3, '/photo.jpg', 4)
                """.trimIndent()
            )
            database.prepare(
                "SELECT captureTimestampMillis, origin FROM media_capture_time WHERE mediaId = 7"
            ).use { statement ->
                statement.step()
                assertEquals(1000L, statement.getLong(0))
                assertEquals("embedded_image", statement.getText(1))
            }
        }
    }
}
