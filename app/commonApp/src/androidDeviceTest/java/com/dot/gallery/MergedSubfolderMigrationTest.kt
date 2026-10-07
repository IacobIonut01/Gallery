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
import com.dot.gallery.data.migration.MIGRATION_41_42
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MergedSubfolderMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        File.createTempFile(
            "merged-subfolder-migration-test",
            ".db",
            instrumentation.targetContext.cacheDir
        ),
        BundledSQLiteDriver(),
        InternalDatabase::class
    )

    @Test
    fun migrate41To42PreservesMergeAndDefaultsToCombined() = runBlocking {
        helper.createDatabase(41).use { db ->
            db.execSQL("INSERT INTO merged_subfolder_table (id) VALUES (123)")
        }

        helper.runMigrationsAndValidate(42, listOf(MIGRATION_41_42)).use { db ->
            db.prepare(
                "SELECT id, folderKey, volume, relativePath, displayMode FROM merged_subfolder_table"
            ).use { statement ->
                statement.step()
                assertEquals(123L, statement.getLong(0))
                assertEquals(true, statement.isNull(1))
                assertEquals("", statement.getText(2))
                assertEquals("", statement.getText(3))
                assertEquals("combined", statement.getText(4))
            }
        }
    }
}
