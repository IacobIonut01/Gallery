package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class EiffelFilter(override val name: String = "Eiffel") : ImageFilter {
    override fun colorMatrix() = ColorMatrix(floatArrayOf(
        0.33f, 0.33f, 0.33f, 0f, 5f,
        0.33f, 0.33f, 0.33f, 0f, 5f,
        0.33f, 0.33f, 0.33f, 0f, 5f,
        0f, 0f, 0f, 1f, 0f
    ))
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
