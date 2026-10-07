/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery

import com.dot.gallery.core.AlbumMediaLoadMode
import com.dot.gallery.core.restorableAlbumTimelineMediaFlow
import com.dot.gallery.data.mediastore.queries.mediaBucketSelection
import com.dot.gallery.data.mediastore.queries.mediaIdsQuery
import com.dot.gallery.data.mediastore.queries.validatedMediaIds
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.app.presentation.util.MockedMediaDistributor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.domain.model.UriMedia


class MediaFlowQueryTest {

    @Test
    fun bucketSelectionIsStableAndDeduplicated() {
        assertEquals("((bucket_id = ?) OR (bucket_id = ?)) OR (bucket_id = ?)", mediaBucketSelection(3))
    }

    @Test
    fun largeBucketSelectionUsesBoundArguments() {
        val selection = mediaBucketSelection(1_500)

        assertEquals(1_500, selection.count { it == '?' })
        assertTrue(selection.contains("bucket_id = ?"))
    }

    @Test
    fun mediaIdsSelectionBindsSortedArguments() {
        assertEquals(listOf(2L, 5L, 9L), validatedMediaIds(setOf(9L, 2L, 5L)))
        assertEquals(
            "((_id = ?) OR (_id = ?)) OR (_id = ?)",
            mediaIdsQuery(listOf(2L, 5L, 9L))?.build()
        )
    }

    @Test
    fun emptyOrAbsentMediaIdsBindNothing() {
        assertEquals(emptyList<Long>(), validatedMediaIds(emptySet()))
        assertNull(validatedMediaIds(null))
    }

    @Test
    fun mediaIdsRejectsOversizedOrNonLocalIds() {
        assertThrows(IllegalArgumentException::class.java) {
            validatedMediaIds((0L..100L).toSet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            validatedMediaIds(setOf(-1L, 5L))
        }
    }

    @Test
    fun largeAlbumRestorationBypassesProgressiveMediaStoreBatch() {
        val distributor = RecordingMediaDistributor()

        distributor.restorableAlbumTimelineMediaFlow(42L)

        assertFalse(AlbumMediaLoadMode.Progressive.skipBatching)
        assertTrue(AlbumMediaLoadMode.Complete.skipBatching)
        assertEquals(42L, distributor.albumId)
        assertEquals(AlbumMediaLoadMode.Complete, distributor.loadMode)
    }

    private class RecordingMediaDistributor : MockedMediaDistributor() {
        var albumId: Long? = null
        var loadMode: AlbumMediaLoadMode? = null

        override fun albumTimelineMediaFlow(
            albumId: Long,
            loadMode: AlbumMediaLoadMode
        ): Flow<MediaState<UriMedia>> {
            this.albumId = albumId
            this.loadMode = loadMode
            return emptyFlow()
        }
    }
}
