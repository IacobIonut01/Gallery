package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature_node.domain.model.editor.TileBehavior
import com.dot.gallery.feature_node.domain.model.editor.TileableAdjustment
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter
import kotlin.math.max

data class Vignette(
    @param:FloatRange(from = 0.0, to = 1.0)
    override val value: Float = 0f
) : VariableFilter, TileableAdjustment {
    override val maxValue = 1f
    override val minValue = 0f
    override val defaultValue = 0f

    override fun apply(bitmap: Bitmap): Bitmap {
        if (value <= 0f) return bitmap
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        drawInto(result, bitmap.width, bitmap.height, 0, 0)
        return result
    }

    /**
     * Renders the exact same full-image radial gradient onto a [tile] that covers the region at
     * ([tileX], [tileY]) of a [fullWidth]x[fullHeight] image — the gradient centre/radius are
     * derived from the full image and the centre is shifted by the tile origin, so the tile is
     * pixel-identical to the corresponding crop of [apply].
     */
    override fun applyTile(
        tile: Bitmap,
        fullWidth: Int,
        fullHeight: Int,
        tileX: Int,
        tileY: Int,
    ): Bitmap {
        if (value <= 0f) return tile
        val result = tile.copy(tile.config ?: Bitmap.Config.ARGB_8888, true)
        drawInto(result, fullWidth, fullHeight, tileX, tileY)
        return result
    }

    private fun drawInto(target: Bitmap, fullWidth: Int, fullHeight: Int, offsetX: Int, offsetY: Int) {
        val canvas = Canvas(target)
        val cx = fullWidth / 2f - offsetX
        val cy = fullHeight / 2f - offsetY
        val radius = max(fullWidth / 2f, fullHeight / 2f) * 1.2f
        val alpha = (value * 200f).toInt().coerceIn(0, 255)
        val gradient = RadialGradient(
            cx, cy, radius,
            intArrayOf(0x00000000, 0x00000000, Color.argb(alpha, 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, 0f, target.width.toFloat(), target.height.toFloat(), paint)
    }

    override fun revert(bitmap: Bitmap): Bitmap = bitmap // Vignette is not easily reversible

    override fun colorMatrix(): ColorMatrix? = null // Cannot be represented as ColorMatrix

    override val tileBehavior: TileBehavior get() = TileBehavior.Analytic
}
