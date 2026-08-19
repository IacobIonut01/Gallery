/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.IntSize
import com.dot.gallery.feature_node.presentation.mediaview.scaledFilmstripFrameWidth
import com.github.panpf.zoomimage.compose.zoom.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPhotoSurfaceTransformTest {

    @Test
    fun preservesImageZoomForMotionVideoSurface() {
        val imageTransform = Transform(
            scale = ScaleFactor(2.5f, 2.5f),
            offset = Offset(84f, -36f),
            scaleOrigin = TransformOrigin(0.25f, 0.75f),
        )

        assertEquals(
            MotionPhotoVideoTransform(
                scaleX = 2.5f,
                scaleY = 2.5f,
                offsetX = 84f,
                offsetY = -36f,
                scaleOrigin = TransformOrigin(0.25f, 0.75f),
            ),
            imageTransform.toMotionPhotoVideoTransform(),
        )
    }

    @Test
    fun unzoomedImageUsesIdentityVideoTransform() {
        assertEquals(
            MotionPhotoVideoTransform.Identity,
            Transform.Origin.toMotionPhotoVideoTransform(),
        )
    }

    @Test
    fun portraitKeyframeWidthIsRoundedInsteadOfTruncated() {
        assertEquals(
            61,
            scaledFilmstripFrameWidth(
                sourceWidth = 1080,
                sourceHeight = 1920,
                targetHeight = 108,
            )
        )
    }

    @Test
    fun portraitFilmstripKeepsNaturalWidthInsteadOfStretchingToLandscapeViewport() {
        assertEquals(
            IntSize(732, 108),
            fitMotionPhotoFilmstrip(
                sourceWidth = 732,
                sourceHeight = 108,
                maxWidth = 1920,
                maxHeight = 108,
            )
        )
    }

    @Test
    fun wideFilmstripShrinksBothDimensionsToPreserveKeyframeAspectRatio() {
        assertEquals(
            IntSize(1909, 89),
            fitMotionPhotoFilmstrip(
                sourceWidth = 2304,
                sourceHeight = 108,
                maxWidth = 1909,
                maxHeight = 108,
            )
        )
    }

    @Test
    fun onlySelectedMotionPhotoOwnsThePlayerSurface() {
        assertTrue(
            shouldRenderMotionPhotoSurface(
                isVideo = false,
                isMotionPhoto = true,
                isSelected = true,
            )
        )
        assertFalse(
            shouldRenderMotionPhotoSurface(
                isVideo = false,
                isMotionPhoto = true,
                isSelected = false,
            )
        )
    }
}
