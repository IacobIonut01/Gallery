package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class ClayFilter(override val name: String = "Clay") : ImageFilter {
    override fun colorMatrix() = ColorMatrix(floatArrayOf(
        1.1f, 0.08f, 0.02f, 0f, 8f,
        0.02f, 1.0f, 0.02f, 0f, 5f,
        0f, 0f, 0.88f, 0f, 5f,
        0f, 0f, 0f, 1f, 0f
    ))
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
