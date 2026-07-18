package com.dot.gallery.feature_node.presentation.edit.adjustments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.scale
import com.dot.gallery.feature_node.domain.model.editor.TileBehavior
import com.dot.gallery.feature_node.domain.model.editor.TileableAdjustment
import com.dot.gallery.feature_node.presentation.util.overlayBitmaps

/**
 * Markup drawn as a transparent [overlay] (strokes, text, blur/mosaic regions) captured at the
 * markup canvas resolution. [apply] composites the overlay onto whatever base [bitmap] it is given,
 * scaling to that base's dimensions — so the same markup reproduces correctly on the interactive
 * proxy AND when replayed onto the full-resolution original by the bake engine (no photo
 * downsampling; only the vector-ish overlay is scaled). [applyTile] composites the correct slice of
 * the full-image-scaled overlay onto a tile without ever allocating a full-resolution overlay.
 */
data class Markup(val overlay: Bitmap): TileableAdjustment {

    override fun apply(bitmap: Bitmap): Bitmap {
        val scaled = if (overlay.width == bitmap.width && overlay.height == bitmap.height) {
            overlay
        } else {
            overlay.scale(bitmap.width, bitmap.height)
        }
        return overlayBitmaps(bitmap, scaled)
    }

    override fun applyTile(
        tile: Bitmap,
        fullWidth: Int,
        fullHeight: Int,
        tileX: Int,
        tileY: Int,
    ): Bitmap {
        val result = tile.copy(tile.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        // Map the overlay onto the full image, then shift into this tile's coordinate space. The
        // canvas clips to the tile bounds, so only the relevant slice is drawn — equivalent to
        // cropping the tile region out of apply()'s full-size composite.
        val matrix = Matrix().apply {
            setScale(
                fullWidth / overlay.width.toFloat(),
                fullHeight / overlay.height.toFloat()
            )
            postTranslate(-tileX.toFloat(), -tileY.toFloat())
        }
        canvas.drawBitmap(overlay, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return result
    }

    override val tileBehavior: TileBehavior get() = TileBehavior.Markup

}