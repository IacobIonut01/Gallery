/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.core.util.or
import com.dot.gallery.domain.uri.path


class StartupCachePolicyTest {

    private val stamp = StartupCacheStamp("generation-1", PERMISSION_MASK_FULL_MEDIA)

    @Test
    fun `matching stamp and full grant is usable`() {
        assertTrue(isStampUsable(stamp, stamp.copy()))
    }

    @Test
    fun `missing stored stamp is a miss`() {
        assertFalse(isStampUsable(null, stamp))
    }

    @Test
    fun `unavailable current stamp is a miss`() {
        assertFalse(isStampUsable(stamp, null))
    }

    @Test
    fun `changed media version is a miss`() {
        assertFalse(isStampUsable(stamp, stamp.copy(mediaVersion = "generation-2")))
    }

    @Test
    fun `changed mounted volumes inside the version string are a miss`() {
        val current = stamp.copy(mediaVersion = "${stamp.mediaVersion}|volumes=external,sd")
        assertFalse(isStampUsable(stamp, current))
    }

    @Test
    fun `changed permission mask is a miss`() {
        assertFalse(isStampUsable(stamp, stamp.copy(permissionMask = PERMISSION_MASK_LEGACY_STORAGE)))
    }

    @Test
    fun `api 33 plus requires both media grants`() {
        assertEquals(
            PERMISSION_MASK_FULL_MEDIA,
            fullReadPermissionMask(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                imagesGranted = true,
                videosGranted = true,
                legacyGranted = false
            )
        )
    }

    @Test
    fun `api 33 plus images only or videos only is never cached`() {
        assertNull(
            fullReadPermissionMask(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                imagesGranted = true,
                videosGranted = false,
                legacyGranted = true
            )
        )
        assertNull(
            fullReadPermissionMask(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                imagesGranted = false,
                videosGranted = true,
                legacyGranted = true
            )
        )
    }

    @Test
    fun `api 30 to 32 uses the legacy storage grant`() {
        assertEquals(
            PERMISSION_MASK_LEGACY_STORAGE,
            fullReadPermissionMask(
                sdkInt = Build.VERSION_CODES.S,
                imagesGranted = false,
                videosGranted = false,
                legacyGranted = true
            )
        )
        assertNull(
            fullReadPermissionMask(
                sdkInt = Build.VERSION_CODES.S,
                imagesGranted = false,
                videosGranted = false,
                legacyGranted = false
            )
        )
    }

    @Test
    fun `api 29 and below is never cached`() {
        assertNull(
            fullReadPermissionMask(
                sdkInt = Build.VERSION_CODES.Q,
                imagesGranted = true,
                videosGranted = true,
                legacyGranted = true
            )
        )
    }

    private fun mediaEntries(count: Int, startId: Long = 1L): List<Pair<Long, String>> =
        (startId until startId + count).map { it to "content://media/$it" }

    @Test
    fun `250 media entries fit the bound`() {
        assertTrue(validateCachedMediaEntries(mediaEntries(MAX_CACHED_MEDIA)))
    }

    @Test
    fun `251 media entries are rejected`() {
        assertFalse(validateCachedMediaEntries(mediaEntries(MAX_CACHED_MEDIA + 1)))
    }

    @Test
    fun `empty media payload is a valid known-empty snapshot`() {
        assertTrue(validateCachedMediaEntries(emptyList()))
    }

    @Test
    fun `non-local media id is rejected`() {
        assertFalse(validateCachedMediaEntries(listOf(-5L to "content://media/5")))
    }

    @Test
    fun `cloud media uri is rejected`() {
        assertFalse(validateCachedMediaEntries(listOf(5L to "cloud://IMMICH/asset-1")))
    }

    @Test
    fun `media uri id mismatch is rejected`() {
        assertFalse(validateCachedMediaEntries(listOf(5L to "content://media/7")))
    }

    @Test
    fun `non media store uris are rejected`() {
        assertNull(mediaStoreUriId("https://example.com/5"))
        assertNull(mediaStoreUriId("file:///storage/5"))
        assertNull(mediaStoreUriId(""))
        assertNull(mediaStoreUriId("content://other/external/images/media/5"))
        assertNull(mediaStoreUriId("content://media/external/images/media/abc"))
        assertNull(mediaStoreUriId("content://media/external/images/media/-5"))
        assertFalse(validateCachedMediaEntries(listOf(5L to "https://example.com/5")))
        assertFalse(validateCachedMediaEntries(listOf(5L to "file:///storage/5")))
        assertFalse(validateCachedMediaEntries(listOf(5L to "")))
        assertFalse(
            validateCachedMediaEntries(
                listOf(5L to "content://other/external/images/media/5")
            )
        )
    }

    @Test
    fun `duplicate media ids are rejected`() {
        assertFalse(
            validateCachedMediaEntries(
                listOf(7L to "content://media/7", 7L to "content://media/8")
            )
        )
    }

    private fun albumEntries(count: Int, startId: Long = 1L): List<CachedStartupAlbum> =
        (startId until startId + count).map {
            CachedStartupAlbum(
                id = it,
                label = "album-$it",
                uri = "content://media/$it",
                pathToThumbnail = "/storage/emulated/0/DCIM/a$it.jpg",
                relativePath = "DCIM/",
                timestamp = it,
                count = 1L,
                size = 1L,
                storageVolume = "external_primary"
            )
        }

    @Test
    fun `512 albums fit the bound`() {
        assertTrue(validateCachedAlbums(albumEntries(MAX_CACHED_ALBUMS)))
    }

    @Test
    fun `513 albums are rejected`() {
        assertFalse(validateCachedAlbums(albumEntries(MAX_CACHED_ALBUMS + 1)))
    }

    @Test
    fun `empty album payload is a valid known-empty snapshot`() {
        assertTrue(validateCachedAlbums(emptyList()))
    }

    @Test
    fun `negative local album id is accepted`() {
        val signedHashId = albumEntries(1).first().copy(id = -4_182_733_917L)
        assertTrue(signedHashId.id < 0)
        assertTrue(validateCachedAlbums(listOf(signedHashId)))
    }

    @Test
    fun `negative album count or size is rejected`() {
        val bad = albumEntries(1).first()
        assertFalse(validateCachedAlbums(listOf(bad.copy(count = -1))))
        assertFalse(validateCachedAlbums(listOf(bad.copy(size = -1))))
    }

    @Test
    fun `cloud album relative path is rejected`() {
        assertFalse(
            validateCachedAlbums(
                listOf(albumEntries(1).first().copy(relativePath = "cloud/IMMICH"))
            )
        )
    }

    @Test
    fun `duplicate album ids are rejected`() {
        val album = albumEntries(1).first()
        assertFalse(validateCachedAlbums(listOf(album, album.copy(label = "other"))))
    }
}
