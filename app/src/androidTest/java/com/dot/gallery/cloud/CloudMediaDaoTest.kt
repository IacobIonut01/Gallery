/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.domain.util.isVideo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the cloud_media query surface that backs the unified timeline and the
 * Favorites/Archive/Trash screens:
 *  - the timeline query excludes trashed AND archived items (the exact source the
 *    MediaDistributor merges into the main grid);
 *  - per-account/per-provider scoping is correct so removing one account can't wipe another;
 *  - toUriMedia() derives strictly-negative, globally-unique ids (timeline-key crash guard).
 */
@RunWith(AndroidJUnit4::class)
class CloudMediaDaoTest {

    private lateinit var db: InternalDatabase
    private lateinit var dao: CloudMediaDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.getCloudMediaDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun media(
        remoteId: String,
        provider: ProviderType,
        configId: Long,
        timestamp: Long = 1_000L,
        favorite: Boolean = false,
        trashed: Boolean = false,
        archived: Boolean = false,
        mimeType: String = "image/jpeg",
        duration: String? = null
    ) = CloudMediaEntity(
        remoteId = remoteId,
        providerType = provider,
        serverConfigId = configId,
        label = remoteId,
        timestamp = timestamp,
        favorite = favorite,
        trashed = trashed,
        archived = archived,
        mimeType = mimeType,
        duration = duration
    )

    @Test
    fun timelineExcludesTrashedAndArchived() = runBlocking {
        dao.insertAll(
            listOf(
                media("visible-1", ProviderType.IMMICH, 1L),
                media("trashed-1", ProviderType.IMMICH, 1L, trashed = true),
                media("archived-1", ProviderType.IMMICH, 1L, archived = true),
                media("visible-2", ProviderType.OWNCLOUD, 2L)
            )
        )

        val timeline = dao.getAllForTimeline().first()
        assertEquals(2, timeline.size)
        assertEquals(setOf("visible-1", "visible-2"), timeline.map { it.remoteId }.toSet())
    }

    @Test
    fun timelineOrderedByTimestampDescending() = runBlocking {
        dao.insertAll(
            listOf(
                media("old", ProviderType.IMMICH, 1L, timestamp = 100L),
                media("new", ProviderType.IMMICH, 1L, timestamp = 300L),
                media("mid", ProviderType.IMMICH, 1L, timestamp = 200L)
            )
        )
        val ordered = dao.getAllForTimeline().first().map { it.remoteId }
        assertEquals(listOf("new", "mid", "old"), ordered)
    }

    @Test
    fun deletingOneAccountLeavesOtherAccountsIntact() = runBlocking {
        dao.insertAll(
            listOf(
                media("a", ProviderType.IMMICH, 1L),
                media("b", ProviderType.IMMICH, 1L),
                media("c", ProviderType.OWNCLOUD, 2L)
            )
        )

        dao.deleteByServerConfig(1L)

        assertEquals(0, dao.countByConfig(1L))
        assertEquals(1, dao.countByConfig(2L))
        assertEquals(1, dao.getAllForTimeline().first().size)
    }

    @Test
    fun sameRemoteIdOnTwoAccountsCoexistViaCompositeKey() = runBlocking {
        // Composite PK is (remoteId, providerType, serverConfigId): two Immich servers can both
        // hold an asset with remoteId "1" without one REPLACE-ing the other.
        dao.insertAll(
            listOf(
                media("1", ProviderType.IMMICH, 1L),
                media("1", ProviderType.IMMICH, 2L)
            )
        )
        assertEquals(2, dao.getAllForTimeline().first().size)
    }

    @Test
    fun rowMutationsAreScopedToOwningAccount() = runBlocking {
        val first = media("shared-id", ProviderType.IMMICH, 1L)
        val second = media("shared-id", ProviderType.IMMICH, 2L)
        dao.insertAll(listOf(first, second))

        val matched = dao.updateFavoriteAndCount("shared-id", ProviderType.IMMICH, 2L, true)
        val unmatched = dao.updateFavoriteAndCount("missing-id", ProviderType.IMMICH, 2L, true)

        assertEquals(1, matched)
        assertEquals(0, unmatched)
        assertEquals(false, dao.getByRemoteId("shared-id", ProviderType.IMMICH, 1L)?.favorite)
        assertEquals(true, dao.getByRemoteId("shared-id", ProviderType.IMMICH, 2L)?.favorite)
        assertEquals(first.globalMediaId, dao.getByGlobalMediaId(first.globalMediaId)?.globalMediaId)
    }

    @Test
    fun favoritesArchivedTrashedCountsAreScoped() = runBlocking {
        dao.insertAll(
            listOf(
                media("f", ProviderType.IMMICH, 1L, favorite = true),
                media("ar", ProviderType.IMMICH, 1L, archived = true),
                media("tr", ProviderType.IMMICH, 1L, trashed = true),
                media("plain", ProviderType.IMMICH, 1L)
            )
        )
        assertEquals(1, dao.countFavorites())
        assertEquals(1, dao.countArchived())
        assertEquals(1, dao.countTrashed())
        assertEquals(2, dao.countCached()) // favorite + plain (archived/trashed excluded)
    }

    @Test
    fun toUriMediaProducesNegativeUniqueIds() = runBlocking {
        dao.insertAll(
            listOf(
                media("1", ProviderType.IMMICH, 1L),
                media("1", ProviderType.IMMICH, 2L),
                media("1", ProviderType.OWNCLOUD, 3L)
            )
        )
        val ids = dao.getAllForTimeline().first().map { it.toUriMedia().id }
        assertTrue("all cloud ids must be negative", ids.all { it < 0L })
        assertEquals("ids must be unique across accounts/providers", ids.size, ids.toSet().size)
    }

    @Test
    fun remoteRefreshPreservesLocalBackupRevision() = runBlocking {
        val uploaded = media("asset", ProviderType.IMMICH, 1L, timestamp = 2_000L).copy(
            localCopyPath = "content://media/external/images/media/42",
            size = 1234L,
            syncState = SyncState.SYNCED
        )
        dao.insert(uploaded)

        dao.insertAll(
            listOf(
                media("asset", ProviderType.IMMICH, 1L, timestamp = 3_000L).copy(
                    label = "refreshed.jpg",
                    size = 1234L,
                    syncState = SyncState.REMOTE_ONLY
                )
            )
        )

        val refreshed = dao.getByRemoteId("asset", ProviderType.IMMICH, 1L)!!
        assertEquals("refreshed.jpg", refreshed.label)
        assertEquals(uploaded.localCopyPath, refreshed.localCopyPath)
        assertEquals(uploaded.timestamp, refreshed.timestamp)
        assertEquals(SyncState.SYNCED, refreshed.syncState)
    }

    @Test
    fun videoWithoutDurationStillResolvesAsVideo() = runBlocking {
        // Path providers can't report duration; toUriMedia must default video duration to ""
        // (non-null) so Media.isVideo stays true and it renders in the player, not as a still.
        dao.insert(media("clip", ProviderType.SMB, 1L, mimeType = "video/mp4", duration = null))
        val uriMedia = dao.getAllForTimeline().first().single().toUriMedia()
        assertEquals("", uriMedia.duration)
        assertTrue(uriMedia.isVideo)
    }
}
