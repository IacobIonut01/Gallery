package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class HoneyFilter(override val name: String = "Honey") : ImageFilter {
    override fun colorMatrix() = ColorMatrix(floatArrayOf(
        1.2f, 0.1f, 0f, 0f, 10f,
        0f, 1.05f, 0f, 0f, 8f,
        0f, 0f, 0.8f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
