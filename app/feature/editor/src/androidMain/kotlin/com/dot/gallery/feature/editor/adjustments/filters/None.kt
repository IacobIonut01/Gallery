package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.model.ImageFilter



data class None(override val name: String = "None") : ImageFilter {

    override fun colorMatrix(): ColorMatrix? = null

    override fun apply(bitmap: Bitmap): Bitmap = bitmap

}