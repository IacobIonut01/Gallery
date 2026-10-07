package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class VogueFilter(override val name: String = "Vogue") : ImageFilter {
    override fun colorMatrix(): ColorMatrix {
        val contrast = 1.25f
        return ColorMatrix(floatArrayOf(
            0.35f * contrast, 0.33f * contrast, 0.32f * contrast, 0f, 128f * (1 - contrast) + 3f,
            0.33f * contrast, 0.34f * contrast, 0.33f * contrast, 0f, 128f * (1 - contrast) + 2f,
            0.32f * contrast, 0.33f * contrast, 0.33f * contrast, 0f, 128f * (1 - contrast),
            0f, 0f, 0f, 1f, 0f
        ))
    }
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
