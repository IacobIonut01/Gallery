package com.dot.gallery.feature_node.presentation.edit.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.awxkee.aire.Aire
import com.dot.gallery.feature_node.domain.model.editor.ImageFilter

data class Monochrome(override val name: String = "Monochrome") : ImageFilter {

    override fun colorMatrix(): ColorMatrix = ColorMatrix(
        floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    override fun apply(bitmap: Bitmap): Bitmap =
        Aire.monochrome(bitmap, floatArrayOf(0.33f, 0.33f, 0.33f, 1f))
}