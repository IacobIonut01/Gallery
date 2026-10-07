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
import com.dot.gallery.data.migration.MIGRATION_43_44
import com.dot.gallery.data.migration.MIGRATION_44_45
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CloudAlbumSyncMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        File.createTempFile(
            "cloud-album-sync-migration-test",
            ".db",
            instrumentation.targetContext.cacheDir
        ),
        BundledSQLiteDriver(),
        InternalDatabase::class
    )

    @Test
    fun migrate43To44PreservesRowsAndAddsAccountToPrimaryKey() = runBlocking {
        helper.createDatabase(43).use { database ->
            database.execSQL(
                """
                INSERT INTO cloud_album_sync
                    (albumRemoteId, providerType, serverConfigId, albumName, syncEnabled)
                VALUES ('album-1', 'IMMICH', 7, 'Camera', 0)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(44, listOf(MIGRATION_43_44)).use { database ->
            database.prepare(
                """
                SELECT albumRemoteId, providerType, serverConfigId, albumName, syncEnabled
                FROM cloud_album_sync
                """.trimIndent()
            ).use { statement ->
                statement.step()
                assertEquals("album-1", statement.getText(0))
                assertEquals("IMMICH", statement.getText(1))
                assertEquals(7L, statement.getLong(2))
                assertEquals("Camera", statement.getText(3))
                assertEquals(0, statement.getInt(4))
            }
        }
    }

    @Test
    fun migrate44To45PreservesContentVerifiedLocalRevisions() = runBlocking {
        helper.createDatabase(44).use { database ->
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

        helper.runMigrationsAndValidate(45, listOf(MIGRATION_44_45)).use { database ->
            database.prepare(
                """
                SELECT serverConfigId, providerType, localUri, localSize, localTimestamp,
                    remoteId, remoteFingerprint, verifiedAt
                FROM cloud_backup_revision
                """.trimIndent()
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals(7L, statement.getLong(0))
                assertEquals("IMMICH", statement.getText(1))
                assertEquals("content://media/42", statement.getText(2))
                assertEquals(1234L, statement.getLong(3))
                assertEquals(100000L, statement.getLong(4))
                assertEquals("asset-1", statement.getText(5))
                assertEquals("sha1", statement.getText(6))
                assertEquals(true, statement.getLong(7) > 0L)
                // Exactly one revision row survives the migration.
                assertEquals(false, statement.step())
            }
        }
    }
}
