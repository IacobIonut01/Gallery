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
import com.dot.gallery.feature_node.data.data_source.migration.MIGRATION_42_43
import com.dot.gallery.feature_node.domain.util.FloatVectorCodec
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun migrate42To43PreservesSmartResultsWithoutRescan() {
        val vector = FloatArray(512) { 1f / kotlin.math.sqrt(512f) }
        val jsonVector = Json.encodeToString(vector)
        val faceBytes = FloatVectorCodec.encode(vector)
        helper.createDatabase(TEST_DB, 42).use { db ->
            db.execSQL(
                "INSERT INTO image_embeddings (id, date, embedding, resultRevision) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(7L, 100L, jsonVector, "clip-v2:model")
            )
            db.execSQL(
                """
                INSERT INTO categories (
                    id, name, searchTerms, embedding, referenceImageIds, threshold,
                    isUserCreated, isPinned, createdAt, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(9L, "Nature", "nature", jsonVector, "[]", 0.2, 0, 0, 10L, 11L)
            )
            db.execSQL(
                """
                INSERT INTO media_category (
                    mediaId, categoryId, similarityScore, addedAt, isManuallyAdded, resultRevision
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(7L, 9L, 0.8, 12L, 0, "categories-v2:model")
            )
            db.execSQL(
                """
                INSERT INTO people (
                    id, name, providerType, thumbnailMediaId, thumbnailUrl,
                    faceCount, lastUpdated, hidden
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>("local_person", "Person", "LOCAL_PEOPLE", 7L, "thumb", 1, 13L, 0)
            )
            db.execSQL(
                """
                INSERT INTO detected_faces (
                    mediaId, personId, embedding, left, top, right, bottom,
                    confidence, timestamp, resultRevision
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(7L, "local_person", faceBytes, 0.1, 0.1, 0.8, 0.8, 0.9, 100L, "face-v2:model")
            )
            db.execSQL(
                """
                INSERT INTO media_feature_state (
                    mediaId, feature, status, sourceRevision, resultRevision, attemptCount, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(7L, "search_embedding", "succeeded", "source", "clip-v2:model", 1, 14L)
            )
            db.execSQL(
                "INSERT INTO image_embeddings (id, date, embedding, resultRevision) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(8L, 100L, "not-json", "clip-v2:model")
            )
            db.execSQL(
                """
                INSERT INTO media_feature_state (
                    mediaId, feature, status, sourceRevision, resultRevision, attemptCount, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(8L, "search_embedding", "succeeded", "source", "clip-v2:model", 1, 14L)
            )
            db.execSQL(
                """
                INSERT INTO smart_scan_runs (
                    runId, trigger, requestedFeatures, userVisible, fullRefresh, status,
                    requestedAt, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>("active", "automatic", 2, 0, 0, "queued", 15L, 15L)
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 43, true, MIGRATION_42_43).use { db ->
            db.query("SELECT embedding, resultRevision, typeof(embedding) FROM image_embeddings WHERE id = 7").use { cursor ->
                cursor.moveToFirst()
                val migrated = FloatVectorCodec.decode(cursor.getBlob(0))
                assertTrue(migrated.contentEquals(vector))
                assertEquals("clip-v2:model", cursor.getString(1))
                assertEquals("blob", cursor.getString(2))
            }
            db.query("SELECT embedding, typeof(embedding) FROM categories WHERE id = 9").use { cursor ->
                cursor.moveToFirst()
                assertTrue(FloatVectorCodec.decode(cursor.getBlob(0)).contentEquals(vector))
                assertEquals("blob", cursor.getString(1))
            }
            db.query("SELECT resultRevision FROM media_category WHERE mediaId = 7 AND categoryId = 9").use { cursor ->
                cursor.moveToFirst()
                assertEquals("categories-v2:model", cursor.getString(0))
            }
            db.query("SELECT status, resultRevision FROM media_feature_state WHERE mediaId = 7").use { cursor ->
                cursor.moveToFirst()
                assertEquals("succeeded", cursor.getString(0))
                assertEquals("clip-v2:model", cursor.getString(1))
            }
            db.query("SELECT centroid, faceCount FROM face_clusters WHERE personId = 'local_person'").use { cursor ->
                cursor.moveToFirst()
                assertTrue(FloatVectorCodec.decode(cursor.getBlob(0)).contentEquals(vector))
                assertEquals(1, cursor.getInt(1))
            }
            db.query("SELECT status, resultRevision FROM media_feature_state WHERE mediaId = 8").use { cursor ->
                cursor.moveToFirst()
                assertEquals("pending", cursor.getString(0))
                assertEquals("", cursor.getString(1))
            }
            db.query("SELECT COUNT(*) FROM image_embeddings WHERE id = 8").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT status FROM smart_scan_runs WHERE runId = 'active'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("queued", cursor.getString(0))
            }
            db.query("PRAGMA foreign_key_check").use { cursor ->
                assertEquals(0, cursor.count)
            }
        }
    }

    private companion object {
        const val TEST_DB = "smart-scan-migration-test"
    }
}
