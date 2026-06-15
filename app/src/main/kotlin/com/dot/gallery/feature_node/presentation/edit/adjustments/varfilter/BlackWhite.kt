package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter
import com.dot.gallery.feature_node.presentation.util.applyColorMatrix

/**
 * Black & white conversion with adjustable intensity.
 * [value] 0f = original colour, 1f = full grayscale.
 */
data class BlackWhite(
    @param:FloatRange(from = 0.0, to = 1.0)
    override val value: Float = 0f
) : VariableFilter {
    override val maxValue = 1f
    override val minValue = 0f
    override val defaultValue = 0f

    override fun apply(bitmap: Bitmap): Bitmap {
        if (value <= 0f) return bitmap
        return applyColorMatrix(bitmap, colorMatrix().values)
    }

    override fun revert(bitmap: Bitmap): Bitmap = bitmap

    override fun colorMatrix(): ColorMatrix {
        // Desaturate by [value]; s = 1f keeps colour, s = 0f is grayscale.
        val s = (1f - value).coerceIn(0f, 1f)
        return ColorMatrix(
            floatArrayOf(
                0.213f * (1 - s) + s, 0.715f * (1 - s), 0.072f * (1 - s), 0f, 0f,
                0.213f * (1 - s), 0.715f * (1 - s) + s, 0.072f * (1 - s), 0f, 0f,
                0.213f * (1 - s), 0.715f * (1 - s), 0.072f * (1 - s) + s, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
}
