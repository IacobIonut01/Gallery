package com.dot.gallery.feature_node.presentation.edit.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.awxkee.aire.Aire
import com.dot.gallery.feature_node.domain.model.editor.ImageFilter
import com.dot.gallery.feature_node.presentation.util.to3x3Matrix

data class Posterize(override val name: String = "Posterize") : ImageFilter {

    override fun colorMatrix(): ColorMatrix = ColorMatrix(
        floatArrayOf(
            0.5f, 0f, 0f, 0f, 0f,
            0f, 0.5f, 0f, 0f, 0f,
            0f, 0f, 0.5f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    override fun apply(bitmap: Bitmap): Bitmap =
        Aire.colorMatrix(bitmap, colorMatrix().values.to3x3Matrix())
}