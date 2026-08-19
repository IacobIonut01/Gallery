package com.dot.gallery.feature_node.data

import android.net.Uri
import com.dot.gallery.feature_node.data.data_source.mediastore.MediaQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreVolumeUriTest {

    @Test
    fun imageUsesExactVolume() {
        assertEquals(
            "content://media/5216-4f19/images/media/42",
            MediaQuery.mediaStoreItemUri(
                id = 42,
                mimeType = "image/jpeg",
                pathOrDisplayName = "photo.jpg",
                volumeName = "5216-4f19"
            ).toString()
        )
    }

    @Test
    fun videoUsesExactVolume() {
        assertEquals(
            "content://media/external_primary/video/media/7",
            MediaQuery.mediaStoreItemUri(
                id = 7,
                mimeType = "video/mp4",
                pathOrDisplayName = "clip.mp4",
                volumeName = "external_primary"
            ).toString()
        )
    }

    @Test
    fun unclassifiedImageUsesFilesCollectionOnExactVolume() {
        assertEquals(
            "content://media/5216-4f19/file/11",
            MediaQuery.mediaStoreItemUri(
                id = 11,
                mimeType = "image/jxl",
                pathOrDisplayName = "photo.jxl",
                volumeName = "5216-4f19"
            ).toString()
        )
    }

    @Test
    fun identifiesFilesCollectionItemsForDirectMutation() {
        assertTrue(MediaQuery.isFilesCollectionUri(Uri.parse("content://media/external_primary/file/11")))
        assertTrue(MediaQuery.isFilesCollectionUri(Uri.parse("content://media/5216-4f19/file/11")))
        assertFalse(MediaQuery.isFilesCollectionUri(Uri.parse("content://media/external_primary/images/media/11")))
        assertFalse(MediaQuery.isFilesCollectionUri(Uri.parse("content://other/external_primary/file/11")))
    }

    @Test
    fun sameIdOnDifferentVolumesProducesDifferentUris() {
        val primary = MediaQuery.mediaStoreItemUri(9, "image/png", "image.png", "external_primary")
        val removable = MediaQuery.mediaStoreItemUri(9, "image/png", "image.png", "5216-4f19")

        assertEquals("content://media/external_primary/images/media/9", primary.toString())
        assertEquals("content://media/5216-4f19/images/media/9", removable.toString())
    }
}
