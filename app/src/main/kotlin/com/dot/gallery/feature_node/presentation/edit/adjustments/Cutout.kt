package com.dot.gallery.feature_node.presentation.edit.adjustments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.scale
import com.dot.gallery.feature_node.domain.model.editor.TileBehavior
import com.dot.gallery.feature_node.domain.model.editor.TileableAdjustment

/**
 * Background removal captured as a full-canvas alpha [mask] at the interactive proxy resolution:
 * the mask's alpha channel is the subject silhouette (opaque = keep, transparent = remove). [apply]
 * multiplies the base bitmap's alpha by the (scaled) mask alpha via a DST_IN composite, so the same
 * cut-out reproduces on the proxy AND when replayed onto the full-resolution original by the bake
 * engine (only the vector-ish mask is scaled; the photo is never downsampled). [applyTile]
 * composites the correct slice of the full-image-scaled mask onto a tile without ever allocating a
 * full-resolution mask.
 */
data class Cutout(val mask: Bitmap) : TileableAdjustment {

    override fun apply(bitmap: Bitmap): Bitmap {
        val scaled = if (mask.width == bitmap.width && mask.height == bitmap.height) {
            mask
        } else {
            mask.scale(bitmap.width, bitmap.height)
        }
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.drawBitmap(scaled, 0f, 0f, Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })
        return result
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
        val matrix = Matrix().apply {
            setScale(
                fullWidth / mask.width.toFloat(),
                fullHeight / mask.height.toFloat()
            )
            postTranslate(-tileX.toFloat(), -tileY.toFloat())
        }
        canvas.drawBitmap(mask, matrix, Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })
        return result
    }

    override val tileBehavior: TileBehavior get() = TileBehavior.Markup
}
