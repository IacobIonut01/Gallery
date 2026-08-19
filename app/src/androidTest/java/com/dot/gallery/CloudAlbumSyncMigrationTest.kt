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
import com.dot.gallery.feature_node.data.data_source.migration.MIGRATION_43_44
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudAlbumSyncMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InternalDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate43To44PreservesRowsAndAddsAccountToPrimaryKey() {
        helper.createDatabase(TEST_DB, 43).use { database ->
            database.execSQL(
                """
                INSERT INTO cloud_album_sync
                    (albumRemoteId, providerType, serverConfigId, albumName, syncEnabled)
                VALUES ('album-1', 'IMMICH', 7, 'Camera', 0)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 44, true, MIGRATION_43_44).use { database ->
            database.query(
                """
                SELECT albumRemoteId, providerType, serverConfigId, albumName, syncEnabled
                FROM cloud_album_sync
                """.trimIndent()
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("album-1", cursor.getString(0))
                assertEquals("IMMICH", cursor.getString(1))
                assertEquals(7L, cursor.getLong(2))
                assertEquals("Camera", cursor.getString(3))
                assertEquals(0, cursor.getInt(4))
            }
        }
    }

    private companion object {
        const val TEST_DB = "cloud-album-sync-migration-test"
    }
}
