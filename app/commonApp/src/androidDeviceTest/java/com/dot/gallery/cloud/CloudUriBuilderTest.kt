/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.cloud.core.CloudUri
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.ThumbnailSize
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.cloud.image.CloudMediaFetcher


/**
 * Round-trips the `cloud://` URI builders in [CloudMediaFetcher] through [CloudUri.parse] to
 * guarantee every pipeline (Sketch/Glide/ExoPlayer/ZoomImage) reconstructs the exact same
 * provider / remoteId / size / fileId / account (cfg) it was built with — including path-based
 * remote ids that contain slashes.
 */
@RunWith(AndroidJUnit4::class)
class CloudUriBuilderTest {

    @Test
    fun buildUriPreviewRoundTrips() {
        val uri = CloudMediaFetcher.buildUri(
            ProviderType.IMMICH, "asset-1", ThumbnailSize.PREVIEW, configId = 5L
        )
        val parsed = CloudUri.parse(uri)!!
        assertEquals(ProviderType.IMMICH, parsed.providerType)
        assertEquals("asset-1", parsed.remoteId)
        assertEquals("preview", parsed.size)
        assertEquals(5L, parsed.configId)
    }

    @Test
    fun buildUriThumbnailWithFileIdRoundTrips() {
        val uri = CloudMediaFetcher.buildUri(
            ProviderType.NEXTCLOUD, "Photos/IMG.jpg", ThumbnailSize.THUMBNAIL, fileId = "42", configId = 9L
        )
        val parsed = CloudUri.parse(uri)!!
        assertEquals(ProviderType.NEXTCLOUD, parsed.providerType)
        assertEquals("Photos/IMG.jpg", parsed.remoteId)
        assertEquals("thumbnail", parsed.size)
        assertEquals("42", parsed.fileId)
        assertEquals(9L, parsed.configId)
    }

    @Test
    fun buildOriginalUriRoundTrips() {
        val uri = CloudMediaFetcher.buildOriginalUri(ProviderType.SMB, "gallery/videos/clip.mp4", configId = 3L)
        val parsed = CloudUri.parse(uri)!!
        assertEquals(ProviderType.SMB, parsed.providerType)
        assertEquals("gallery/videos/clip.mp4", parsed.remoteId)
        assertEquals("original", parsed.size)
        assertEquals(3L, parsed.configId)
    }

    @Test
    fun buildPersonUriRoundTrips() {
        val uri = CloudMediaFetcher.buildPersonUri(ProviderType.IMMICH, "person-7", configId = 2L)
        val parsed = CloudUri.parse(uri)!!
        assertEquals("person-7", parsed.remoteId)
        assertEquals("person", parsed.typeParam)
        assertEquals(2L, parsed.configId)
    }

    @Test
    fun noConfigIdOmitsCfgParam() {
        val uri = CloudMediaFetcher.buildOriginalUri(ProviderType.WEBDAV, "x/y.jpg")
        val parsed = CloudUri.parse(uri)!!
        assertEquals(-1L, parsed.configId)
    }
}
