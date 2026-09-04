package com.dot.gallery

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.feature_node.presentation.edit.adjustments.Markup
import com.dot.gallery.feature_node.presentation.edit.bake.EditReplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkupAdjustmentTest {
    @Test
    fun replayCompositesMarkupOntoProxyAndFullResolutionBases() {
        val proxyBase = solidBitmap(4, 4, Color.BLUE)
        val overlay = solidBitmap(4, 4, Color.TRANSPARENT).apply {
            for (y in 0 until height) {
                for (x in 0 until width / 2) {
                    setPixel(x, y, Color.argb(128, 255, 0, 0))
                }
            }
        }
        val adjustment = Markup(overlay)
        val liveProxy = adjustment.apply(proxyBase)
        val fullBaseSoftware = solidBitmap(8, 8, Color.GREEN)
        val fullBase = fullBaseSoftware.copy(Bitmap.Config.HARDWARE, false)
        val fullResult = EditReplay.replay(fullBase, listOf(adjustment))

        try {
            assertTrue(EditReplay.matchesLiveResult(proxyBase, listOf(adjustment), liveProxy))
            assertNotEquals(Color.GREEN, fullResult.getPixel(0, 0))
            assertEquals(Color.GREEN, fullResult.getPixel(7, 7))
        } finally {
            proxyBase.recycle()
            overlay.recycle()
            liveProxy.recycle()
            fullBaseSoftware.recycle()
            fullBase.recycle()
            fullResult.recycle()
        }
    }

    @Test
    fun tiledReplayUsesTheMatchingFullImageOverlayRegion() {
        val overlay = solidBitmap(4, 4, Color.TRANSPARENT).apply {
            for (y in 0 until height) {
                for (x in width / 2 until width) {
                    setPixel(x, y, Color.RED)
                }
            }
        }
        val tile = solidBitmap(2, 4, Color.GREEN)
        val result = Markup(overlay).applyTile(
            tile = tile,
            fullWidth = 4,
            fullHeight = 4,
            tileX = 2,
            tileY = 0,
        )

        try {
            for (y in 0 until result.height) {
                for (x in 0 until result.width) {
                    assertEquals(Color.RED, result.getPixel(x, y))
                }
            }
        } finally {
            overlay.recycle()
            tile.recycle()
            result.recycle()
        }
    }

    @Test
    fun hardwareOverlayStillCompositesOntoSoftwareBases() {
        // GraphicsLayer.toImageBitmap() returns a HARDWARE bitmap on some devices; a software
        // Canvas silently ignores those, which used to make the markup a no-op (#1078).
        val softwareOverlay = solidBitmap(4, 4, Color.TRANSPARENT).apply {
            for (y in 0 until height) {
                for (x in 0 until width / 2) {
                    setPixel(x, y, Color.RED)
                }
            }
        }
        val overlay = softwareOverlay.copy(Bitmap.Config.HARDWARE, false)
        val base = solidBitmap(4, 4, Color.GREEN)
        val tile = solidBitmap(2, 4, Color.GREEN)
        val adjustment = Markup(overlay)
        val result = adjustment.apply(base)
        val tileResult = adjustment.applyTile(tile, fullWidth = 4, fullHeight = 4, tileX = 0, tileY = 0)

        try {
            assertEquals(Bitmap.Config.HARDWARE, overlay.config)
            assertFalse(result.sameAs(base))
            assertEquals(Color.RED, result.getPixel(0, 0))
            assertEquals(Color.GREEN, result.getPixel(3, 3))
            assertEquals(Color.RED, tileResult.getPixel(0, 0))
            assertEquals(Color.RED, tileResult.getPixel(1, 3))
        } finally {
            softwareOverlay.recycle()
            overlay.recycle()
            base.recycle()
            tile.recycle()
            result.recycle()
            tileResult.recycle()
        }
    }

    private fun solidBitmap(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
}
