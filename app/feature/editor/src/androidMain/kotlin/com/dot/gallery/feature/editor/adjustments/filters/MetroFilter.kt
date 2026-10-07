package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class MetroFilter(override val name: String = "Metro") : ImageFilter {
    override fun colorMatrix(): ColorMatrix {
        val contrast = 1.3f
        return ColorMatrix(floatArrayOf(
            contrast * 0.95f, 0f, 0.05f, 0f, 128f * (1 - contrast),
            0f, contrast * 0.98f, 0.02f, 0f, 128f * (1 - contrast),
            0f, 0.02f, contrast * 1.05f, 0f, 128f * (1 - contrast),
            0f, 0f, 0f, 1f, 0f
        ))
    }
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
