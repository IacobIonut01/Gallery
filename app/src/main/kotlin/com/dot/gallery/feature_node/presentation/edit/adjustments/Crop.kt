package com.dot.gallery.feature_node.presentation.edit.adjustments

import android.graphics.Bitmap
import com.dot.gallery.feature_node.domain.model.editor.Adjustment

data class Crop(val newBitmap: Bitmap): Adjustment {

    override fun apply(bitmap: Bitmap): Bitmap {
        return newBitmap
    }

}