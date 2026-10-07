package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class VistaFilter(override val name: String = "Vista") : ImageFilter {
    override fun colorMatrix(): ColorMatrix {
        val contrast = 0.85f
        return ColorMatrix(floatArrayOf(
            0.33f * contrast, 0.33f * contrast, 0.33f * contrast, 0f, 128f * (1 - contrast) + 15f,
            0.33f * contrast, 0.33f * contrast, 0.33f * contrast, 0f, 128f * (1 - contrast) + 15f,
            0.33f * contrast, 0.33f * contrast, 0.33f * contrast, 0f, 128f * (1 - contrast) + 15f,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
