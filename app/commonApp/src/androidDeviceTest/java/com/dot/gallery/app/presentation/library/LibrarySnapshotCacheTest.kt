/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.library

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.cloudMediaId
import com.dot.gallery.core.startup.StartupCacheStamp
import com.dot.gallery.domain.model.LibraryIndicatorState
import com.dot.gallery.domain.model.LocationMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.startup.StartupMediaCache
import com.dot.gallery.feature.library.ui.CloudLibraryState
import com.dot.gallery.feature.library.ui.LibraryCategoryPreview
import com.dot.gallery.feature.library.ui.LibraryGeoPreview
import com.dot.gallery.feature.library.ui.LibraryScrollPosition
import com.dot.gallery.feature.library.ui.LibrarySnapshot
import com.dot.gallery.feature.library.ui.LibraryViewport
import com.dot.gallery.cloud.core.LOCAL_PEOPLE_CONFIG_ID
import com.dot.gallery.core.startup.PERMISSION_MASK_FULL_MEDIA
import com.dot.gallery.domain.uri.path


@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LibrarySnapshotCacheTest {

    private val fingerprint = "fp-v1"
    private lateinit var context: Context
    private lateinit var testFile: File
    private var scopeJob = SupervisorJob()
    private var stamp: StartupCacheStamp? = StartupCacheStamp("v1", PERMISSION_MASK_FULL_MEDIA)
    private lateinit var cache: StartupMediaCache

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testFile = File(context.filesDir, "startup_library_test_${System.nanoTime()}.pb")
        cache = createCache { stamp }
    }

    @After
    fun tearDown() {
        runBlocking { scopeJob.cancelAndJoin() }
        testFile.delete()
    }

    private fun createCache(
        provider: suspend () -> StartupCacheStamp?
    ) = StartupMediaCache(
        context,
        testFile,
        CoroutineScope(scopeJob + Dispatchers.IO),
        provider
    )

    private suspend fun recycle(
        provider: suspend () -> StartupCacheStamp? = { stamp }
    ) {
        scopeJob.cancelAndJoin()
        scopeJob = SupervisorJob()
        cache = createCache(provider)
    }

    private fun media(id: Long) = UriMedia(
        id = id,
        label = "photo-$id.jpg",
        uri = Uri.parse("content://media/external/images/media/$id"),
        path = "/storage/emulated/0/DCIM/photo-$id.jpg",
        relativePath = "DCIM/",
        albumID = 10L,
        albumLabel = "Camera",
        timestamp = 1_700_000_000L + id,
        fullDate = "2023-11-14",
        mimeType = "image/jpeg",
        favorite = 0,
        trashed = 0,
        size = 1_024L
    )

    private fun cloudMedia(configId: Long, remoteId: String) = UriMedia(
        id = cloudMediaId(ProviderType.IMMICH, configId, remoteId),
        label = "$remoteId.jpg",
        uri = Uri.parse("cloud://IMMICH/$remoteId?cfg=$configId&size=thumbnail"),
        path = "cloud://IMMICH/$remoteId",
        relativePath = "cloud/",
        albumID = -1L,
        albumLabel = "Remote",
        timestamp = 1_700_000_000L,
        fullDate = "2023-11-14",
        mimeType = "image/jpeg",
        favorite = 0,
        trashed = 0,
        size = 2_048L
    )

    private fun person(
        id: String,
        configId: Long = 11L,
        type: ProviderType = ProviderType.IMMICH,
        thumb: String? = null
    ) = PersonInfo(
        id = id,
        name = "person-$id",
        providerType = type,
        serverConfigId = configId,
        thumbnailUrl = thumb,
        assetCount = 4
    )

    private fun snapshot(
        locations: List<LocationMedia>? = listOf(
            LocationMedia(media = media(1L), location = "Berlin", latitude = 52.5, longitude = 13.4)
        ),
        locationCount: Int = locations?.size ?: 0,
        people: List<PersonInfo> = listOf(
            person("a", 11L, thumb = "cloud://IMMICH/face-a?cfg=11&type=person"),
            person("b", LOCAL_PEOPLE_CONFIG_ID, ProviderType.LOCAL_PEOPLE)
        ),
        peopleCount: Int = 9,
        viewport: LibraryViewport = LibraryViewport(
            grid = LibraryScrollPosition("grid-key", 6, 24),
            locations = LibraryScrollPosition("1", 0, 10),
            people = LibraryScrollPosition("IMMICH/11/a", 0, 0),
            configuration = "sig-1"
        )
    ) = LibrarySnapshot(
        categories = listOf(
            LibraryCategoryPreview(
                id = 5L,
                name = "Nature",
                mediaCount = 12,
                thumbnailMedia = media(2L)
            )
        ),
        categoryCount = 12,
        locations = locations,
        locationCount = locationCount,
        latestGeo = LibraryGeoPreview(media(3L), 40.0, -74.0),
        peopleCount = peopleCount,
        peopleCountsByAccount = mapOf(11L to 5, LOCAL_PEOPLE_CONFIG_ID to 4),
        sharedLinkCountsByAccount = mapOf(11L to 3),
        cloud = CloudLibraryState(
            hasCloud = true,
            hasCachedMedia = true,
            archivedCount = 2,
            totalCloudCount = 40,
            people = people,
            sharedLinkCount = 3
        ),
        indicators = LibraryIndicatorState(trashCount = 3, favoriteCount = 8),
        viewport = viewport
    )

    @Test
    fun libraryRoundtripSurvivesANewInstanceWithBothLocalAndCloudPreviews() = runTest {
        val original = snapshot(
            locations = listOf(
                LocationMedia(
                    media = media(1L),
                    location = "Berlin",
                    latitude = 52.5,
                    longitude = 13.4
                ),
                LocationMedia(
                    media = cloudMedia(11L, "remote-1"),
                    location = "Oslo",
                    latitude = 59.9,
                    longitude = 10.7
                )
            ),
            locationCount = 2
        ).copy(
            categories = listOf(
                LibraryCategoryPreview(
                    id = 5L,
                    name = "Nature",
                    mediaCount = 12,
                    thumbnailMedia = media(2L)
                ),
                LibraryCategoryPreview(
                    id = 6L,
                    name = "Remote",
                    mediaCount = 4,
                    thumbnailMedia = cloudMedia(11L, "remote-2")
                )
            )
        )
        cache.writeLibrary(stamp, fingerprint, original)
        recycle()

        val read = cache.readLibrary(fingerprint)
        assertNotNull(read)
        read!!
        assertEquals(original.categories, read.categories)
        assertEquals(original.categoryCount, read.categoryCount)
        assertEquals(original.locations, read.locations)
        assertEquals(original.locationCount, read.locationCount)
        assertEquals(original.latestGeo, read.latestGeo)
        assertEquals(original.peopleCount, read.peopleCount)
        assertEquals(original.peopleCountsByAccount, read.peopleCountsByAccount)
        assertEquals(original.sharedLinkCountsByAccount, read.sharedLinkCountsByAccount)
        assertEquals(original.cloud.people, read.cloud.people)
        assertEquals(original.indicators, read.indicators)
        assertEquals(original.viewport, read.viewport)
        assertFalse(read.cloud.isConnected)
        assertTrue(read.cloud.connectedCapabilities.isEmpty())

        val raw = testFile.readBytes()
        assertFalse(String(raw, Charsets.UTF_8).contains("person-a"))
    }

    @Test
    fun locationWindowIsBoundedAroundTheSavedAnchor() = runTest {
        val many = (0 until 600).map { i ->
            LocationMedia(
                media = media(1_000L + i),
                location = "loc-$i",
                latitude = 10.0,
                longitude = 20.0
            )
        }
        val anchorId = 1_555L
        val original = snapshot(
            locations = many,
            locationCount = 600,
            viewport = LibraryViewport(
                locations = LibraryScrollPosition(anchorId.toString(), 555, 0)
            )
        )
        cache.writeLibrary(stamp, fingerprint, original)
        recycle()

        val read = cache.readLibrary(fingerprint)
        assertNotNull(read)
        read!!
        assertEquals(64, read.locations!!.size)
        assertEquals(600, read.locationCount)
        val windowIds = read.locations!!.map { it.media.id }
        assertTrue(anchorId in windowIds)
    }

    @Test
    fun peopleWindowIsBoundedButTrueCountsPersist() = runTest {
        val many = (0 until 200).map { person("p$it", 11L) }
        val original = snapshot(
            people = many,
            peopleCount = 430,
            viewport = LibraryViewport(
                people = LibraryScrollPosition("IMMICH/11/p100", 100, 0)
            )
        ).copy(peopleCountsByAccount = mapOf(11L to 430))
        cache.writeLibrary(stamp, fingerprint, original)
        recycle()

        val read = cache.readLibrary(fingerprint)
        assertNotNull(read)
        read!!
        assertEquals(64, read.cloud.people.size)
        assertEquals(430, read.peopleCount)
        assertEquals(mapOf(11L to 430), read.peopleCountsByAccount)
        assertTrue(read.cloud.people.any { it.id == "p100" })
    }

    @Test
    fun fingerprintMismatchIsAMiss() = runTest {
        cache.writeLibrary(stamp, fingerprint, snapshot())
        recycle()
        assertNull(cache.readLibrary("fp-other"))
        assertNotNull(cache.readLibrary(fingerprint))
    }

    @Test
    fun changedStampIsAMiss() = runTest {
        cache.writeLibrary(stamp, fingerprint, snapshot())
        recycle { StartupCacheStamp("v2", PERMISSION_MASK_FULL_MEDIA) }
        assertNull(cache.readLibrary(fingerprint))
    }

    @Test
    fun nullStampSkipsTheWrite() = runTest {
        cache.writeLibrary(null, fingerprint, snapshot())
        recycle()
        assertNull(cache.readLibrary(fingerprint))
    }

    @Test
    fun corruptedPayloadIsAMiss() = runTest {
        cache.writeLibrary(stamp, fingerprint, snapshot())
        recycle()
        assertNotNull(cache.readLibrary(fingerprint))

        cache.store.edit {
            it[stringPreferencesKey("startup_library")] = "[{\"peopleCount\":"
        }
        assertNull(cache.readLibrary(fingerprint))
    }

    @Test
    fun negativeCountersRefuseToWrite() = runTest {
        cache.writeLibrary(
            stamp, fingerprint, snapshot().copy(peopleCount = -1)
        )
        recycle()
        assertNull(cache.readLibrary(fingerprint))

        cache.writeLibrary(
            stamp,
            fingerprint,
            snapshot().copy(peopleCountsByAccount = mapOf(11L to -2))
        )
        recycle()
        assertNull(cache.readLibrary(fingerprint))
    }

    @Test
    fun remoteOnlyThumbnailUrlsArePersistedAndUnsafeUrlsAreNulled() = runTest {
        val original = snapshot(
            people = listOf(
                person("safe", 11L, thumb = "cloud://IMMICH/face?cfg=11&type=person"),
                person("remote-http", 11L, thumb = "https://example.com/t.jpg"),
                person("wrong-account", 11L, thumb = "cloud://IMMICH/face?cfg=99"),
                person("local-file", LOCAL_PEOPLE_CONFIG_ID, ProviderType.LOCAL_PEOPLE)
            )
        )
        cache.writeLibrary(stamp, fingerprint, original)
        recycle()

        val read = cache.readLibrary(fingerprint)
        assertNotNull(read)
        val byId = read!!.cloud.people.associateBy { it.id }
        assertEquals(
            "cloud://IMMICH/face?cfg=11&type=person",
            byId.getValue("safe").thumbnailUrl
        )
        assertNull(byId.getValue("remote-http").thumbnailUrl)
        assertNull(byId.getValue("wrong-account").thumbnailUrl)
    }

    @Test
    fun minimalSnapshotStillRoundtrips() = runTest {
        val bad = snapshot(
            locations = null,
            locationCount = 0
        ).copy(latestGeo = null)
        cache.writeLibrary(stamp, fingerprint, bad)
        recycle()
        assertNotNull(cache.readLibrary(fingerprint))
    }
}
