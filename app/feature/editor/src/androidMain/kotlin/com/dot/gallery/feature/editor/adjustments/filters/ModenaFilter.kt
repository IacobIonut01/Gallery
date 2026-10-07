package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class ModenaFilter(override val name: String = "Modena") : ImageFilter {
    override fun colorMatrix() = ColorMatrix(floatArrayOf(
        1.15f, 0.05f, 0f, 0f, 5f,
        0.02f, 1.08f, 0f, 0f, 5f,
        0f, 0f, 0.95f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
