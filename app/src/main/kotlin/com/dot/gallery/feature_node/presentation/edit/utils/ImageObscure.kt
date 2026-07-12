package com.dot.gallery.feature_node.presentation.edit.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.scale

/**
 * RenderScript-free bitmap obscuring helpers used by the blur/mosaic markup brushes.
 *
 * Both effects work by downscaling and re-upscaling the source: a bilinear round-trip produces a
 * gaussian-like blur, while a nearest-neighbour up-scale produces hard mosaic blocks. This is cheap
 * (no per-pixel convolution), works on every supported API level and is applied at display
 * resolution for previews.
 */
object ImageObscure {

    /**
     * Gaussian-ish blur. [strength] in 0..1 controls how aggressively the image is shrunk before
     * being scaled back up (stronger = smaller intermediate = blurrier).
     */
    fun blur(src: Bitmap, strength: Float): Bitmap {
        val s = strength.coerceIn(0f, 1f)
        // Shrink factor 4x .. ~40x across the slider range.
        val factor = 4f + s * 36f
        val w = (src.width / factor).toInt().coerceAtLeast(1)
        val h = (src.height / factor).toInt().coerceAtLeast(1)
        val small = src.scale(w, h, filter = true)
        return small.scale(src.width, src.height, filter = true).also {
            if (small != it) small.recycle()
        }
    }

    /**
     * Mosaic / pixelate. [strength] in 0..1 controls the block size (stronger = larger blocks).
     */
    fun mosaic(src: Bitmap, strength: Float): Bitmap {
        val s = strength.coerceIn(0f, 1f)
        // Target block size in source pixels: ~1.5% .. ~12% of the longest edge.
        val longest = maxOf(src.width, src.height)
        val block = (longest * (0.015f + s * 0.105f)).toInt().coerceAtLeast(2)
        val w = (src.width / block).coerceAtLeast(1)
        val h = (src.height / block).coerceAtLeast(1)
        val small = src.scale(w, h, filter = true)
        val out = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        // Nearest-neighbour up-scale => crisp blocks.
        val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
        val matrix = android.graphics.Matrix().apply {
            setScale(src.width.toFloat() / w, src.height.toFloat() / h)
        }
        canvas.drawBitmap(small, matrix, paint)
        small.recycle()
        return out
    }
}
