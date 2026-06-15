package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.ColorMatrix
import androidx.core.graphics.createBitmap
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter
import kotlin.math.sqrt

/**
 * Edge-detection effect using a Sobel operator on the luminance channel.
 * [value] 0f = original image, 1f = full edge map blended in.
 * Implemented per-pixel, so it has no colour matrix.
 */
data class Edges(
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
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return bitmap

        val src = IntArray(width * height)
        bitmap.getPixels(src, 0, width, 0, 0, width, height)

        // Precompute luminance for each pixel
        val lum = FloatArray(width * height)
        for (i in src.indices) {
            val c = src[i]
            val r = c ushr 16 and 0xFF
            val g = c ushr 8 and 0xFF
            val b = c and 0xFF
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        val out = IntArray(width * height)
        val blend = value.coerceIn(0f, 1f)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    out[idx] = blendEdge(src[idx], 0f, blend)
                    continue
                }
                val tl = lum[idx - width - 1]
                val tc = lum[idx - width]
                val tr = lum[idx - width + 1]
                val ml = lum[idx - 1]
                val mr = lum[idx + 1]
                val bl = lum[idx + width - 1]
                val bc = lum[idx + width]
                val br = lum[idx + width + 1]

                val gx = (tr + 2 * mr + br) - (tl + 2 * ml + bl)
                val gy = (bl + 2 * bc + br) - (tl + 2 * tc + tr)
                val mag = sqrt(gx * gx + gy * gy).coerceIn(0f, 255f)
                out[idx] = blendEdge(src[idx], mag, blend)
            }
        }

        val result = createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, width, 0, 0, width, height)
        return result
    }

    private fun blendEdge(original: Int, magnitude: Float, blend: Float): Int {
        val a = original ushr 24 and 0xFF
        val or = original ushr 16 and 0xFF
        val og = original ushr 8 and 0xFF
        val ob = original and 0xFF
        val e = magnitude.toInt().coerceIn(0, 255)
        val r = (or * (1 - blend) + e * blend).toInt().coerceIn(0, 255)
        val g = (og * (1 - blend) + e * blend).toInt().coerceIn(0, 255)
        val b = (ob * (1 - blend) + e * blend).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
