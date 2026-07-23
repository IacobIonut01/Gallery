package com.dot.gallery.feature_node.presentation.edit.adjustments

import android.graphics.Bitmap
import android.graphics.Canvas
import com.dot.gallery.feature_node.domain.model.editor.Adjustment

/**
 * Composites a (possibly transparent) bitmap over a solid [color], producing an opaque result so a
 * cut-out edit can be saved in a format without an alpha channel (e.g. JPEG). Per-pixel — the bake
 * engine applies it directly to each tile.
 */
data class FlattenBackground(val color: Int) : Adjustment {

    override fun apply(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(color)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return result
    }
}
