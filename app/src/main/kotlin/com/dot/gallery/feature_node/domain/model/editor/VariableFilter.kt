package com.dot.gallery.feature_node.domain.model.editor

import android.graphics.Bitmap
import androidx.annotation.Keep
import androidx.compose.ui.graphics.ColorMatrix

@Keep
interface VariableFilter : Adjustment {

    val maxValue: Float
    val minValue: Float
    val defaultValue: Float
    val value: Float

    fun revert(bitmap: Bitmap): Bitmap

    fun colorMatrix(): ColorMatrix?

}