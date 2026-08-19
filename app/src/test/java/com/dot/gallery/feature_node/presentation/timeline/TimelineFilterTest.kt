/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.timeline

import com.dot.gallery.feature_node.domain.model.MediaTypeFilter
import com.dot.gallery.feature_node.domain.model.TimelineFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineFilterTest {
    @Test
    fun mergedAlbumSelectionExpandsToSourceAlbumIds() {
        assertEquals(
            setOf(10L, 11L),
            resolveTimelineAlbumSourceIds(
                selectedAlbumIds = setOf(99L),
                sourceIdsByAlbumId = mapOf(99L to setOf(10L, 11L)),
            )
        )
    }

    @Test
    fun unsupportedAlbumSelectionDoesNotHideAllMedia() {
        val items = listOf(TestMedia(1L, albumId = 10L), TestMedia(2L, albumId = 11L))
        val filter = TimelineFilter(selectedAlbumIds = setOf(-500L))

        assertEquals(
            items,
            filter(items, filter, selectedAlbumSourceIds = emptySet()),
        )
    }

    @Test
    fun activeFiltersApplyTogether() {
        val matching = TestMedia(
            id = 1L,
            albumId = 10L,
            image = true,
            favorite = true,
            timestampSeconds = 1_704_067_200L,
        )
        val items = listOf(
            matching,
            matching.copy(id = 2L, favorite = false),
            matching.copy(id = 3L, image = false, video = true),
            matching.copy(id = 4L, albumId = 12L),
        )
        val filter = TimelineFilter(
            mediaType = MediaTypeFilter.PHOTOS,
            favoritesOnly = true,
            selectedYears = setOf(2024),
            selectedAlbumIds = setOf(99L),
        )

        assertEquals(listOf(matching), filter(items, filter, setOf(10L, 11L)))
    }

    private fun filter(
        items: List<TestMedia>,
        filter: TimelineFilter,
        selectedAlbumSourceIds: Set<Long>,
    ) = filterTimelineItems(
        items = items,
        filter = filter,
        selectedAlbumSourceIds = selectedAlbumSourceIds,
        isImage = TestMedia::image,
        isVideo = TestMedia::video,
        isFavorite = TestMedia::favorite,
        timestampSeconds = TestMedia::timestampSeconds,
        albumId = TestMedia::albumId,
    )

    private data class TestMedia(
        val id: Long,
        val albumId: Long,
        val image: Boolean = true,
        val video: Boolean = false,
        val favorite: Boolean = false,
        val timestampSeconds: Long = 0L,
    )
}
