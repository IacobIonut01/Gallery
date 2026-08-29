package com.dot.gallery

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter.Borders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BordersAdjustmentTest {
    @Test
    fun addsAnotherBorderToPreviouslySavedHardwareBitmap() {
        val source = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val previouslyBordered = Borders(value = 0.5f, color = Color.WHITE).apply(source)
        val reopened = previouslyBordered.copy(Bitmap.Config.HARDWARE, false)
        val result = Borders(value = 0.5f, color = Color.BLACK).apply(reopened)
        val addedThickness = (minOf(reopened.width, reopened.height) * 0.5f * 0.12f).toInt()

        try {
            assertTrue(result.width > reopened.width)
            assertTrue(result.height > reopened.height)
            assertNotEquals(Bitmap.Config.HARDWARE, result.config)
            assertEquals(Color.BLACK, result.getPixel(0, 0))
            assertEquals(Color.WHITE, result.getPixel(addedThickness, addedThickness))
            assertEquals(Color.RED, result.getPixel(result.width / 2, result.height / 2))
        } finally {
            source.recycle()
            previouslyBordered.recycle()
            reopened.recycle()
            result.recycle()
        }
    }
}
