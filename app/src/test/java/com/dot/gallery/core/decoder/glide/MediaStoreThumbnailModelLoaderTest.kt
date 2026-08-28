/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.glide

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreThumbnailModelLoaderTest {

    @Test
    fun embeddedFrontCameraThumbnailBypassesPlatformLoader() {
        assertTrue(
            shouldBypassPlatformThumbnail(
                hasEmbeddedThumbnail = true,
                lensModel = "Pixel 10 Pro front camera 2.713mm f/2.2",
            )
        )
    }

    @Test
    fun frontCameraWithoutEmbeddedThumbnailUsesPlatformLoader() {
        assertFalse(
            shouldBypassPlatformThumbnail(
                hasEmbeddedThumbnail = false,
                lensModel = "Pixel 10 Pro front camera 2.713mm f/2.2",
            )
        )
    }

    @Test
    fun embeddedRearCameraThumbnailUsesPlatformLoader() {
        assertFalse(
            shouldBypassPlatformThumbnail(
                hasEmbeddedThumbnail = true,
                lensModel = "Pixel 10 Pro back camera 6.9mm f/1.68",
            )
        )
    }
}
