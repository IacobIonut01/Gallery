package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class BlushFilter(override val name: String = "Blush") : ImageFilter {
    override fun colorMatrix() = ColorMatrix(floatArrayOf(
        1.1f, 0.05f, 0.05f, 0f, 12f,
        0f, 0.98f, 0f, 0f, 5f,
        0.02f, 0f, 1.02f, 0f, 8f,
        0f, 0f, 0f, 1f, 0f
    ))
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
