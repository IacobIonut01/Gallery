/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.data.InternalDatabase
import com.dot.gallery.data.migration.MIGRATION_46_47
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CloudTagMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        File.createTempFile(
            "cloud-tag-migration-test",
            ".db",
            instrumentation.targetContext.cacheDir
        ),
        BundledSQLiteDriver(),
        InternalDatabase::class
    )

    @Test
    fun migrate46To47CreatesTagTables() = runBlocking {
        helper.createDatabase(46).close()

        helper.runMigrationsAndValidate(47, listOf(MIGRATION_46_47)).use { database ->
            database.execSQL(
                """
                INSERT INTO cloud_tag
                    (serverConfigId, providerType, tagId, name, value, color, lastSyncedAt)
                VALUES (7, 'IMMICH', 'tag-1', 'Nature', 'Nature', '#12ab34', 100000)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO cloud_media_tag
                    (serverConfigId, providerType, tagId, remoteId)
                VALUES (7, 'IMMICH', 'tag-1', 'asset-1')
                """.trimIndent()
            )
            database.prepare("SELECT tagId, name, color FROM cloud_tag").use { statement ->
                assertTrue(statement.step())
                assertEquals("tag-1", statement.getText(0))
                assertEquals("Nature", statement.getText(1))
                assertEquals("#12ab34", statement.getText(2))
                assertEquals(false, statement.step())
            }
            database.prepare(
                "SELECT remoteId FROM cloud_media_tag WHERE tagId = 'tag-1'"
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("asset-1", statement.getText(0))
                assertEquals(false, statement.step())
            }
            // The (serverConfigId, tagId) primary key must actually reject duplicates.
            var duplicateRejected = false
            try {
                database.execSQL(
                    """
                    INSERT INTO cloud_tag
                        (serverConfigId, providerType, tagId, name, value, color, lastSyncedAt)
                    VALUES (7, 'IMMICH', 'tag-1', 'Dup', 'Dup', NULL, 100001)
                    """.trimIndent()
                )
            } catch (e: SQLiteException) {
                duplicateRejected = true
            }
            assertTrue("cloud_tag primary key must reject duplicate (serverConfigId, tagId)", duplicateRejected)
        }
    }
}
