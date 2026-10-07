package com.dot.gallery.feature.editor.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.ColorMatrix

@Keep
interface ImageFilter : Adjustment {

    fun colorMatrix(): ColorMatrix?
}