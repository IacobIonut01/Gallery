/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.domain.model.Album
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
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
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.uri.path


@RunWith(AndroidJUnit4::class)
class StartupMediaCacheTest {

    private lateinit var context: Context
    private lateinit var testFile: File
    private var scopeJob = SupervisorJob()
    private var stamp: StartupCacheStamp? = StartupCacheStamp("v1", PERMISSION_MASK_FULL_MEDIA)
    private lateinit var cache: StartupMediaCache

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testFile = File(context.filesDir, "startup_cache_test_${System.nanoTime()}.pb")
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

    private fun album(id: Long) = Album(
        id = id,
        label = "album-$id",
        uri = Uri.parse("content://media/external/images/media/$id"),
        pathToThumbnail = "/storage/emulated/0/Pictures/a$id.jpg",
        relativePath = "Pictures/",
        timestamp = 1_700_000_000L + id,
        count = 12L + id,
        size = 4_096L + id,
        storageVolume = "external_primary"
    )

    @Test
    fun nothingWrittenIsAMiss() = runTest {
        assertNull(cache.readMedia())
        assertNull(cache.readAlbums())
    }

    @Test
    fun mediaRoundtripIsBoundedEncryptedAndSurvivesANewInstance() = runTest {
        val items = (1L..300L).map(::media)
        cache.writeMedia(stamp, items)
        recycle()

        val read = cache.readMedia()
        assertNotNull(read)
        assertEquals(MAX_CACHED_MEDIA, read!!.size)
        assertEquals((1L..250L).toList(), read.map { it.id })
        assertEquals(items.first(), read.first())

        val raw = testFile.readBytes()
        assertTrue(raw.isNotEmpty())
        val asText = String(raw, Charsets.UTF_8)
        assertFalse(asText.contains("content://media"))
        assertFalse(asText.contains("photo-"))
        assertFalse(asText.contains("DCIM"))
    }

    @Test
    fun albumFieldsRoundtripAcrossInstances() = runTest {
        val albums = listOf(album(1L), album(2L))
        cache.writeAlbums(stamp, albums)
        recycle()

        val read = cache.readAlbums()
        assertNotNull(read)
        assertEquals(2, read!!.size)
        val expected = albums[1]
        val actual = read[1]
        assertEquals(expected.id, actual.id)
        assertEquals(expected.label, actual.label)
        assertEquals(expected.uri.toString(), actual.uri.toString())
        assertEquals(expected.pathToThumbnail, actual.pathToThumbnail)
        assertEquals(expected.relativePath, actual.relativePath)
        assertEquals(expected.timestamp, actual.timestamp)
        assertEquals(expected.count, actual.count)
        assertEquals(expected.size, actual.size)
        assertEquals(expected.storageVolume, actual.storageVolume)
    }

    @Test
    fun negativeLocalAlbumIdRoundtrips() = runTest {
        val albums = listOf(album(1L).copy(id = -418_273_917L), album(2L))
        cache.writeAlbums(stamp, albums)
        recycle()

        val read = cache.readAlbums()
        assertNotNull(read)
        assertEquals(listOf(-418_273_917L, 2L), read!!.map { it.id })
    }

    @Test
    fun emptySnapshotIsDistinctFromMiss() = runTest {
        cache.writeMedia(stamp, emptyList())
        cache.writeAlbums(stamp, emptyList())
        recycle()
        assertEquals(emptyList<UriMedia>(), cache.readMedia())
        assertEquals(emptyList<Album>(), cache.readAlbums())
    }

    @Test
    fun corruptedPayloadIsAMiss() = runTest {
        cache.writeMedia(stamp, listOf(media(1L)))
        recycle()
        assertNotNull(cache.readMedia())

        cache.store.edit {
            it[stringPreferencesKey("startup_media")] = "[{\"id\":1,\"label\":\"photo"
        }
        assertNull(cache.readMedia())
    }

    @Test
    fun truncatedCiphertextIsAMiss() = runTest {
        cache.writeMedia(stamp, listOf(media(1L)))
        recycle()
        assertNotNull(cache.readMedia())

        scopeJob.cancelAndJoin()
        scopeJob = SupervisorJob()
        val raw = testFile.readBytes()
        assertTrue(raw.size > 16)
        testFile.writeBytes(raw.copyOf(raw.size / 2))
        cache = createCache { stamp }

        assertNull(cache.readMedia())
    }

    @Test
    fun oversizedPayloadReadIsAMiss() = runTest {
        cache.writeMedia(stamp, listOf(media(1L)))
        recycle()

        cache.store.edit {
            it[stringPreferencesKey("startup_media")] = "x".repeat(MAX_PAYLOAD_CHARS + 1)
        }
        assertNull(cache.readMedia())
    }

    @Test
    fun oversizedPayloadWriteIsSkipped() = runTest {
        val huge = (1L..250L).map { media(it).copy(label = "x".repeat(5_000) + it) }
        cache.writeMedia(stamp, huge)
        recycle()
        assertNull(cache.readMedia())
    }

    @Test
    fun changedStampIsAMiss() = runTest {
        cache.writeMedia(stamp, listOf(media(1L)))
        stamp = StartupCacheStamp("v2", PERMISSION_MASK_FULL_MEDIA)
        recycle()
        assertNull(cache.readMedia())
    }

    @Test
    fun stampChangeDuringReadIsAMiss() = runTest {
        var calls = 0
        val evolved = StartupCacheStamp("v2", PERMISSION_MASK_FULL_MEDIA)
        recycle {
            calls++
            if (calls >= 3) evolved else stamp
        }
        cache.writeMedia(stamp, listOf(media(1L)))
        assertNull(cache.readMedia())
        assertTrue(calls >= 3)
    }

    @Test
    fun writeWithStaleStampIsSkipped() = runTest {
        cache.writeMedia(
            StartupCacheStamp("bogus-generation", PERMISSION_MASK_FULL_MEDIA),
            listOf(media(1L))
        )
        recycle()
        assertNull(cache.readMedia())
    }

    @Test
    fun oversizedAlbumListRemovesPriorEntry() = runTest {
        cache.writeAlbums(stamp, listOf(album(1L)))
        recycle()
        assertNotNull(cache.readAlbums())

        cache.writeAlbums(stamp, (1L..513L).map(::album))
        recycle()
        assertNull(cache.readAlbums())
    }

    @Test
    fun invalidMediaPayloadIsAMiss() = runTest {
        cache.writeMedia(stamp, listOf(media(1L)))
        cache.store.edit {
            it[stringPreferencesKey("startup_media")] =
                "[{\"id\":-7,\"label\":\"c.jpg\",\"uri\":\"cloud://IMMICH/a1\",\"path\":\"x\",\"relativePath\":\"Immich\",\"albumID\":-500,\"albumLabel\":\"Immich\",\"timestamp\":1,\"fullDate\":\"d\",\"mimeType\":\"image/jpeg\",\"favorite\":0,\"trashed\":0,\"size\":1}]"
        }
        assertNull(cache.readMedia())
    }

    @Test
    fun cancellationDuringTheStampReadPropagates() = runTest {
        val entered = CompletableDeferred<Unit>()
        recycle { entered.complete(Unit); awaitCancellation() }
        val job = launch { cache.readMedia() }
        entered.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }

    @Test
    fun cancelledReadDoesNotPublishCachedData() = runTest {
        cache.writeMedia(stamp, listOf(media(1L)))
        val entered = CompletableDeferred<Unit>()
        var calls = 0
        recycle {
            calls++
            if (calls >= 2) {
                entered.complete(Unit)
                awaitCancellation()
            } else stamp
        }
        var published = false
        val job = launch {
            cache.readMedia()
            published = true
        }
        entered.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertFalse(published)
    }

    @Test
    fun cancellationDuringTheWritePropagates() = runTest {
        val entered = CompletableDeferred<Unit>()
        recycle { entered.complete(Unit); awaitCancellation() }
        val job = launch { cache.writeMedia(stamp, listOf(media(1L))) }
        entered.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }
}
