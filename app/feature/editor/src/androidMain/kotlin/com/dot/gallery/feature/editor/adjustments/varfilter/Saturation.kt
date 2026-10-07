package com.dot.gallery.feature.editor.adjustments.varfilter

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.VariableFilter
import com.dot.gallery.feature.editor.util.applyColorMatrix



data class Saturation(
    @param:FloatRange(from = 0.0, to = 2.0)
    override val value: Float = 1f
) : VariableFilter {
    override val maxValue = 2f
    override val minValue = 0f
    override val defaultValue = 1f

    override fun apply(bitmap: Bitmap): Bitmap =
        applyColorMatrix(bitmap, colorMatrix().values)

    override fun revert(bitmap: Bitmap): Bitmap =
        Saturation(1f / value.coerceAtLeast(0.01f)).apply(bitmap)

    override fun colorMatrix(): ColorMatrix =
        ColorMatrix(
            floatArrayOf(
                0.213f * (1 - value) + value, 0.715f * (1 - value), 0.072f * (1 - value), 0f, 0f,
                0.213f * (1 - value), 0.715f * (1 - value) + value, 0.072f * (1 - value), 0f, 0f,
                0.213f * (1 - value), 0.715f * (1 - value), 0.072f * (1 - value) + value, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )

}