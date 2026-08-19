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
import com.dot.gallery.feature_node.data.data_source.migration.MIGRATION_44_45
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

    @Test
    fun migrate44To45PreservesContentVerifiedLocalRevisions() {
        helper.createDatabase(LOCAL_REVISION_DB, 44).use { database ->
            database.execSQL(
                """
                INSERT INTO cloud_media (
                    remoteId, providerType, serverConfigId, globalMediaId, label, path,
                    relativePath, mimeType, timestamp, size, width, height, favorite, trashed,
                    archived, syncState, localCopyPath, contentHash, thumbnailUrl, originalUrl,
                    lastSyncedAt, fileId
                ) VALUES (
                    'asset-1', 'IMMICH', 7, -1, 'photo.jpg', 'photo.jpg', '', 'image/jpeg',
                    100000, 1234, 10, 10, 0, 0, 0, 'SYNCED', 'content://media/42', 'sha1', '', '',
                    100000, '42'
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO cloud_media (
                    remoteId, providerType, serverConfigId, globalMediaId, label, path,
                    relativePath, mimeType, timestamp, size, width, height, favorite, trashed,
                    archived, syncState, localCopyPath, thumbnailUrl, originalUrl, lastSyncedAt,
                    fileId
                ) VALUES (
                    'Photos/photo.jpg', 'WEBDAV', 8, -2, 'photo.jpg', 'Photos/photo.jpg',
                    'Photos', 'image/jpeg', 200000, 1234, 10, 10, 0, 0, 0, 'SYNCED',
                    'content://media/43', '', '', 200000, '43'
                )
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(LOCAL_REVISION_DB, 45, true, MIGRATION_44_45).use { database ->
            database.query(
                """
                SELECT serverConfigId, providerType, localUri, localSize, localTimestamp,
                    remoteId, remoteFingerprint, verifiedAt
                FROM cloud_backup_revision
                """.trimIndent()
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(7L, cursor.getLong(0))
                assertEquals("IMMICH", cursor.getString(1))
                assertEquals("content://media/42", cursor.getString(2))
                assertEquals(1234L, cursor.getLong(3))
                assertEquals(100000L, cursor.getLong(4))
                assertEquals("asset-1", cursor.getString(5))
                assertEquals("sha1", cursor.getString(6))
                assertEquals(true, cursor.getLong(7) > 0L)
            }
        }
    }

    private companion object {
        const val TEST_DB = "cloud-album-sync-migration-test"
        const val LOCAL_REVISION_DB = "cloud-local-revision-migration-test"
    }
}
