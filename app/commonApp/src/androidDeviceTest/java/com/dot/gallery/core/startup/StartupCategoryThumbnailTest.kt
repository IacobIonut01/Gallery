/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import android.net.Uri
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.data.entity.CloudServerConfigEntity
import com.dot.gallery.data.InternalDatabase
import com.dot.gallery.data.repository.combineCategoryThumbnails
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.model.LockedAlbum
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.presentation.components.util.hasMediaAccess
import com.dot.gallery.domain.uri.path


@RunWith(AndroidJUnit4::class)
class StartupCategoryThumbnailTest {

    private lateinit var db: InternalDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun local(id: Long, albumId: Long = 10L, trashed: Int = 0) = UriMedia(
        id = id,
        label = "photo-$id.jpg",
        uri = Uri.parse("content://media/external/images/media/$id"),
        path = "/storage/emulated/0/DCIM/photo-$id.jpg",
        relativePath = "DCIM/",
        albumID = albumId,
        albumLabel = "Camera",
        timestamp = 1_700_000_000L + id,
        fullDate = "2023-11-14",
        mimeType = "image/jpeg",
        favorite = 0,
        trashed = trashed,
        size = 1_024L
    )

    private fun cloud(
        remoteId: String,
        configId: Long,
        trashed: Boolean = false,
        archived: Boolean = false
    ) = CloudMediaEntity(
        remoteId = remoteId,
        providerType = ProviderType.IMMICH,
        serverConfigId = configId,
        label = "asset-$remoteId.jpg",
        path = "album/asset-$remoteId.jpg",
        relativePath = "album",
        mimeType = "image/jpeg",
        timestamp = 1_700_000_000_000L,
        size = 2_048L,
        trashed = trashed,
        archived = archived
    )

    @Test
    fun observeByGlobalMediaIdsExcludesTrashedAndArchived() = runBlocking {
        val keep = cloud("keep", configId = 1L)
        val trashed = cloud("trashed", configId = 1L, trashed = true)
        val archived = cloud("archived", configId = 1L, archived = true)
        db.getCloudMediaDao().insertAllRaw(listOf(keep, trashed, archived))

        val result = db.getCloudMediaDao()
            .observeByGlobalMediaIds(
                listOf(keep.globalMediaId, trashed.globalMediaId, archived.globalMediaId)
            )
            .first()

        assertEquals(listOf(keep.globalMediaId), result.map { it.globalMediaId })
    }

    @Test
    fun cloudThumbnailsAreIsolatedToActiveConfigs() = runBlocking {
        val activeConfig = CloudServerConfigEntity(
            providerType = ProviderType.IMMICH,
            serverUrl = "https://one.example",
            isActive = true
        )
        val inactiveConfig = CloudServerConfigEntity(
            providerType = ProviderType.IMMICH,
            serverUrl = "https://two.example",
            isActive = false
        )
        val activeId = db.getCloudServerConfigDao().insert(activeConfig)
        val inactiveId = db.getCloudServerConfigDao().insert(inactiveConfig)
        val onActive = cloud("shared-asset", activeId)
        val onInactive = cloud("shared-asset", inactiveId)
        db.getCloudMediaDao().insertAllRaw(listOf(onActive, onInactive))

        val cloudRows = db.getCloudMediaDao()
            .observeByGlobalMediaIds(listOf(onActive.globalMediaId, onInactive.globalMediaId))
            .first()
        assertEquals(2, cloudRows.size)

        val activeConfigs = db.getCloudServerConfigDao().getActive().first()
        val combined = combineCategoryThumbnails(
            local = emptyList(),
            cloud = cloudRows,
            activeConfigs = activeConfigs,
            blacklisted = emptyList(),
            locked = emptyList(),
            hasMediaAccess = true
        )

        assertEquals(listOf(onActive.globalMediaId), combined.map { it.id })
        assertTrue(combined.single().uri.toString().contains("cfg=$activeId"))
    }

    @Test
    fun lockedAlbumCoversAreFilteredBeforeEmission() = runBlocking {
        val visible = local(1L, albumId = 10L)
        val locked = local(2L, albumId = 11L)
        db.getLockedAlbumDao().insertLockedAlbum(LockedAlbum(11L))

        val combined = combineCategoryThumbnails(
            local = listOf(visible, locked),
            cloud = emptyList(),
            activeConfigs = emptyList(),
            blacklisted = emptyList(),
            locked = db.getLockedAlbumDao().getLockedAlbums().first(),
            hasMediaAccess = true
        )

        assertEquals(listOf(1L), combined.map { it.id })
    }

    @Test
    fun blacklistedAlbumCoversFollowTimelineRules() = runBlocking {
        val visible = local(1L, albumId = 10L)
        val hidden = local(2L, albumId = 11L)
        val albumsOnlyHidden = local(3L, albumId = 12L)
        db.getBlacklistDao().addBlacklistedAlbum(
            IgnoredAlbum(id = 11L, location = IgnoredAlbum.TIMELINE_ONLY)
        )
        db.getBlacklistDao().addBlacklistedAlbum(
            IgnoredAlbum(id = 12L, location = IgnoredAlbum.ALBUMS_ONLY)
        )

        val combined = combineCategoryThumbnails(
            local = listOf(visible, hidden, albumsOnlyHidden),
            cloud = emptyList(),
            activeConfigs = emptyList(),
            blacklisted = db.getBlacklistDao().getBlacklistedAlbums().first(),
            locked = emptyList(),
            hasMediaAccess = true
        )

        assertEquals(setOf(1L, 3L), combined.mapTo(HashSet()) { it.id })
    }

    @Test
    fun localCoversFailClosedWithoutMediaAccess() {
        val granted = combineCategoryThumbnails(
            local = listOf(local(1L), local(2L)),
            cloud = emptyList(),
            activeConfigs = emptyList(),
            blacklisted = emptyList(),
            locked = emptyList(),
            hasMediaAccess = true
        )
        val revoked = combineCategoryThumbnails(
            local = listOf(local(1L), local(2L)),
            cloud = emptyList(),
            activeConfigs = emptyList(),
            blacklisted = emptyList(),
            locked = emptyList(),
            hasMediaAccess = false
        )

        assertEquals(setOf(1L, 2L), granted.mapTo(HashSet()) { it.id })
        assertTrue(revoked.isEmpty())
    }
}
