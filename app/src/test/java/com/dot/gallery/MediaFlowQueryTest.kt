/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery

import com.dot.gallery.core.AlbumMediaLoadMode
import com.dot.gallery.core.restorableAlbumTimelineMediaFlow
import com.dot.gallery.feature_node.data.data_source.mediastore.queries.mediaBucketSelection
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.presentation.util.MockedMediaDistributor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        ): Flow<MediaState<Media.UriMedia>> {
            this.albumId = albumId
            this.loadMode = loadMode
            return emptyFlow()
        }
    }
}
