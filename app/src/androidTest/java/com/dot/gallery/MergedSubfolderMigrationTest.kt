/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.migration.MIGRATION_41_42
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MergedSubfolderMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InternalDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate41To42PreservesMergeAndDefaultsToCombined() {
        helper.createDatabase(TEST_DB, 41).use { db ->
            db.execSQL("INSERT INTO merged_subfolder_table (id) VALUES (123)")
        }

        helper.runMigrationsAndValidate(TEST_DB, 42, true, MIGRATION_41_42).use { db ->
            db.query(
                "SELECT id, folderKey, volume, relativePath, displayMode FROM merged_subfolder_table"
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(123L, cursor.getLong(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals("", cursor.getString(2))
                assertEquals("", cursor.getString(3))
                assertEquals("combined", cursor.getString(4))
            }
        }
    }

    private companion object {
        const val TEST_DB = "merged-subfolder-migration-test"
    }
}
