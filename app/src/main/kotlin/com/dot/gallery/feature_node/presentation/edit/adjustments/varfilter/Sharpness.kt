package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.ColorMatrix
import com.awxkee.aire.Aire
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter

data class Sharpness(
    @FloatRange(from = -1.0, to = 10.0)
    override val value: Float = 0f
) : VariableFilter {
    override val maxValue = 10f
    override val minValue = -1f
    override val defaultValue = 0f

    override fun apply(bitmap: Bitmap): Bitmap {
        return Aire.sharpness(bitmap, value)
    }

    override fun revert(bitmap: Bitmap): Bitmap {
        return Aire.sharpness(bitmap, -value)
    }

    override fun colorMatrix(): ColorMatrix? = null

}