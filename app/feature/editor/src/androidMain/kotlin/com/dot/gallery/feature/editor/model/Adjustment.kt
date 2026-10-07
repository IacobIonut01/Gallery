package com.dot.gallery.feature.editor.model

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.dot.gallery.feature.editor.util.sentenceCase
import com.dot.gallery.domain.model.editor.TileBehavior

@Keep
interface Adjustment {
    fun apply(bitmap: Bitmap): Bitmap

    val name: String get() = this::class.simpleName.toString().sentenceCase()

    /**
     * How this adjustment is re-applied to the full-resolution original by the tiled bake engine.
     * Defaults to [TileBehavior.PerPixel]; ops that need neighbouring pixels, global geometry, or
     * change dimensions override this so the bake reproduces the proxy result exactly at full res.
     */
    val tileBehavior: TileBehavior get() = TileBehavior.PerPixel

}