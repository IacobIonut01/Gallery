package com.dot.gallery.feature.editor.adjustments

import android.graphics.Bitmap
import com.dot.gallery.feature.editor.model.Adjustment
import com.dot.gallery.domain.model.editor.TileBehavior
import com.dot.gallery.feature.editor.util.rotate



data class Rotate90CW(
    val angle: Float
) : Adjustment {

    override fun apply(bitmap: Bitmap): Bitmap {
        return bitmap.rotate(angle)
    }

    override val tileBehavior: TileBehavior get() = TileBehavior.Geometry

}