package com.dot.gallery.feature_node.domain.model.editor

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.dot.gallery.feature_node.presentation.util.sentenceCase

@Keep
interface Adjustment {
    fun apply(bitmap: Bitmap): Bitmap

    val name: String get() = this::class.simpleName.toString().sentenceCase()

}