package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class OnyxFilter(override val name: String = "Onyx") : ImageFilter {
    override fun colorMatrix(): ColorMatrix {
        val contrast = 1.3f
        val cm = ColorMatrix(floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.timesAssign(ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, 128f * (1 - contrast),
            0f, contrast, 0f, 0f, 128f * (1 - contrast),
            0f, 0f, contrast, 0f, 128f * (1 - contrast),
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
