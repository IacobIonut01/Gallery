package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.ColorMatrix
import androidx.core.graphics.createBitmap
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter

/**
 * Adds a solid border around the image, expanding the canvas.
 * [value] 0f = no border, 1f = thickest (12% of the shortest side).
 * [color] defaults to white. Implemented by redrawing onto a larger canvas,
 * so it has no colour matrix.
 */
data class Borders(
    @param:FloatRange(from = 0.0, to = 1.0)
    override val value: Float = 0f,
    val color: Int = Color.WHITE
) : VariableFilter {
    override val maxValue = 1f
    override val minValue = 0f
    override val defaultValue = 0f

    override fun colorMatrix(): ColorMatrix? = null

    override fun revert(bitmap: Bitmap): Bitmap = bitmap

    override fun apply(bitmap: Bitmap): Bitmap {
        if (value <= 0f) return bitmap
        val thickness = (minOf(bitmap.width, bitmap.height) * value * 0.12f).toInt()
        if (thickness <= 0) return bitmap
        val newWidth = bitmap.width + thickness * 2
        val newHeight = bitmap.height + thickness * 2
        val result = createBitmap(newWidth, newHeight, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(color)
        canvas.drawBitmap(bitmap, thickness.toFloat(), thickness.toFloat(), null)
        return result
    }
}
