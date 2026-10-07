package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class DesertFilter(override val name: String = "Desert") : ImageFilter {
    override fun colorMatrix(): ColorMatrix {
        val cm = ColorMatrix(floatArrayOf(
            1.1f, 0.05f, 0f, 0f, 10f,
            0f, 1.0f, 0f, 0f, 5f,
            0f, 0f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        val s = 0.85f
        cm.timesAssign(ColorMatrix(floatArrayOf(
            0.213f * (1 - s) + s, 0.715f * (1 - s), 0.072f * (1 - s), 0f, 0f,
            0.213f * (1 - s), 0.715f * (1 - s) + s, 0.072f * (1 - s), 0f, 0f,
            0.213f * (1 - s), 0.715f * (1 - s), 0.072f * (1 - s) + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }
    override fun apply(bitmap: Bitmap) = applyViaColorMatrix(bitmap, colorMatrix())
}
