package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter
import com.dot.gallery.feature_node.presentation.util.applyColorMatrix
import kotlin.math.cos
import kotlin.math.sin

/**
 * Luminance-preserving hue rotation.
 * [value] is in -1f..1f and maps linearly to -180°..180°.
 */
data class Hue(
    @param:FloatRange(from = -1.0, to = 1.0)
    override val value: Float = 0f
) : VariableFilter {
    override val maxValue = 1f
    override val minValue = -1f
    override val defaultValue = 0f

    override fun apply(bitmap: Bitmap): Bitmap {
        if (value == 0f) return bitmap
        return applyColorMatrix(bitmap, colorMatrix().values)
    }

    override fun revert(bitmap: Bitmap): Bitmap = Hue(-value).apply(bitmap)

    override fun colorMatrix(): ColorMatrix {
        val angle = (value * 180f) * (Math.PI.toFloat() / 180f)
        val c = cos(angle)
        val s = sin(angle)
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        return ColorMatrix(
            floatArrayOf(
                lumR + c * (1 - lumR) + s * (-lumR), lumG + c * (-lumG) + s * (-lumG), lumB + c * (-lumB) + s * (1 - lumB), 0f, 0f,
                lumR + c * (-lumR) + s * (0.143f), lumG + c * (1 - lumG) + s * (0.140f), lumB + c * (-lumB) + s * (-0.283f), 0f, 0f,
                lumR + c * (-lumR) + s * (-(1 - lumR)), lumG + c * (-lumG) + s * (lumG), lumB + c * (1 - lumB) + s * (lumB), 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
}
