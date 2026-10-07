package com.dot.gallery.feature.editor.adjustments

import android.graphics.Bitmap
import com.dot.gallery.feature.editor.model.Adjustment
import com.dot.gallery.domain.model.editor.TileBehavior
import com.dot.gallery.feature.editor.util.flipHorizontally
import com.dot.gallery.feature.editor.util.flipVertically



data class Flip(
    val horizontal: Boolean,
) : Adjustment {

    override fun apply(bitmap: Bitmap): Bitmap {
        return if (horizontal) bitmap.flipHorizontally() else bitmap.flipVertically()
    }

    override val tileBehavior: TileBehavior get() = TileBehavior.Geometry

}