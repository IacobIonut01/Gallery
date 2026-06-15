package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.ColorMatrix
import androidx.core.graphics.createBitmap
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter

/**
 * Posterize effect: reduces the number of tonal levels per colour channel.
 * [value] 0f = original (256 levels), 1f = strongest (few levels).
 * Implemented as a per-pixel quantization, so it has no colour matrix.
 */
data class Posterize(
    @param:FloatRange(from = 0.0, to = 1.0)
    override val value: Float = 0f
) : VariableFilter {
    override val maxValue = 1f
    override val minValue = 0f
    override val defaultValue = 0f

    override fun colorMatrix(): ColorMatrix? = null

    override fun revert(bitmap: Bitmap): Bitmap = bitmap

    override fun apply(bitmap: Bitmap): Bitmap {
        if (value <= 0f) return bitmap
        // Map 0f..1f -> 24..2 levels so the effect is visible across the whole slider.
        val levels = Math.round(2f + (1f - value) * 22f).coerceIn(2, 24)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Precompute quantization lookup table for 0..255
        val step = 255f / (levels - 1)
        val lut = IntArray(256)
        for (i in 0..255) {
            lut[i] = (Math.round(i / step) * step).toInt().coerceIn(0, 255)
        }

        for (idx in pixels.indices) {
            val color = pixels[idx]
            val a = color ushr 24 and 0xFF
            val r = lut[color ushr 16 and 0xFF]
            val g = lut[color ushr 8 and 0xFF]
            val b = lut[color and 0xFF]
            pixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        val result = createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
