/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.slideshow

import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.SlideshowTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [buildSlideshowOrder] — the deterministic playlist builder that drives
 * the slideshow pager (filter by type, reverse, seeded shuffle, rotate-to-start).
 *
 * Uses [Media.EncryptedMedia] because it carries no [android.net.Uri] field, keeping the test
 * runnable on the plain JVM (no Robolectric/mocked Android framework).
 */
class SlideshowPlaylistTest {

    private fun media(id: Long, mime: String, duration: String? = null): Media.EncryptedMedia =
        Media.EncryptedMedia(
            id = id,
            label = "m$id",
            bytes = ByteArray(0),
            path = "/path/$id",
            relativePath = "/",
            albumID = 1L,
            albumLabel = "album",
            timestamp = id,
            fullDate = "",
            mimeType = mime,
            favorite = 0,
            trashed = 0,
            size = 0L,
            duration = duration
        )

    private fun image(id: Long) = media(id, "image/jpeg")
    private fun gif(id: Long) = media(id, "image/gif")
    private fun video(id: Long) = media(id, "video/mp4", duration = "1000")

    private val cfg = SlideshowConfig(transition = SlideshowTransition.FADE)

    private fun ids(list: List<Media>) = list.map { it.id }

    @Test
    fun keepsOrderAndAllItemsWhenNoFiltersOrReordering() {
        val source = listOf(image(1), image(2), image(3))
        val result = buildSlideshowOrder(source, cfg, startId = null, seed = 0L)
        assertEquals(listOf(1L, 2L, 3L), ids(result))
    }

    @Test
    fun excludesVideosWhenNotIncluded() {
        val source = listOf(image(1), video(2), image(3))
        val result = buildSlideshowOrder(
            source, cfg.copy(includeVideos = false), startId = null, seed = 0L
        )
        assertEquals(listOf(1L, 3L), ids(result))
    }

    @Test
    fun excludesGifsWhenNotIncluded() {
        val source = listOf(image(1), gif(2), image(3))
        val result = buildSlideshowOrder(
            source, cfg.copy(includeGifs = false), startId = null, seed = 0L
        )
        assertEquals(listOf(1L, 3L), ids(result))
    }

    @Test
    fun reverseReversesOrder() {
        val source = listOf(image(1), image(2), image(3))
        val result = buildSlideshowOrder(
            source, cfg.copy(reverse = true), startId = null, seed = 0L
        )
        assertEquals(listOf(3L, 2L, 1L), ids(result))
    }

    @Test
    fun rotatesToStartIdWhenNotRandom() {
        val source = listOf(image(1), image(2), image(3), image(4))
        val result = buildSlideshowOrder(source, cfg, startId = 3L, seed = 0L)
        assertEquals(listOf(3L, 4L, 1L, 2L), ids(result))
    }

    @Test
    fun randomIsDeterministicForSameSeed() {
        val source = (1L..20L).map { image(it) }
        val a = buildSlideshowOrder(source, cfg.copy(random = true), startId = null, seed = 42L)
        val b = buildSlideshowOrder(source, cfg.copy(random = true), startId = null, seed = 42L)
        assertEquals(ids(a), ids(b))
        // Same set of items, just reordered.
        assertEquals(source.map { it.id }.toSet(), ids(a).toSet())
    }

    @Test
    fun randomKeepsStartItemFirst() {
        val source = (1L..20L).map { image(it) }
        val result = buildSlideshowOrder(source, cfg.copy(random = true), startId = 7L, seed = 42L)
        assertEquals(7L, result.first().id)
        assertEquals(source.size, result.size)
    }

    @Test
    fun emptyResultWhenEverythingFilteredOut() {
        val source = listOf(video(1), video(2))
        val result = buildSlideshowOrder(
            source, cfg.copy(includeVideos = false), startId = 1L, seed = 0L
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun deduplicatesById() {
        val source = listOf(image(1), image(1), image(2))
        val result = buildSlideshowOrder(source, cfg, startId = null, seed = 0L)
        assertEquals(listOf(1L, 2L), ids(result))
    }

    @Test
    fun matchesFiltersHelper() {
        assertFalse(video(1).matchesSlideshowFilters(cfg.copy(includeVideos = false)))
        assertFalse(gif(1).matchesSlideshowFilters(cfg.copy(includeGifs = false)))
        assertTrue(image(1).matchesSlideshowFilters(cfg))
    }
}
