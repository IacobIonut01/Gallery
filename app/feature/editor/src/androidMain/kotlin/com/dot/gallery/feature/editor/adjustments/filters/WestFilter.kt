package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter

data class WestFilter(override val name: String = "West") : ImageFilter {
    override fun colorMatrix(): ColorMatrix {
        val s = 0.8f
        val cm = ColorMatrix(floatArrayOf(
            0.95f, 0f, 0.05f, 0f, 5f,
            0f, 0.98f, 0.02f, 0f, 5f,
            0f, 0.05f, 1.05f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        ))
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
