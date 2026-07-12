package com.dot.gallery.feature_node.domain.model.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

class PathProperties(
    var strokeWidth: Float = 20f,
    var color: Color = Color.Red,
    var alpha: Float = 1f,
    var strokeCap: StrokeCap = StrokeCap.Round,
    var strokeJoin: StrokeJoin = StrokeJoin.Round,
    var eraseMode: Boolean = false,
    var brush: MarkupBrush = MarkupBrush.Solid,
    var effectStrength: Float = 0.5f,
    var fillRegion: Boolean = false
) {

    fun copy(
        strokeWidth: Float = this.strokeWidth,
        color: Color = this.color,
        alpha: Float = this.alpha,
        strokeCap: StrokeCap = this.strokeCap,
        strokeJoin: StrokeJoin = this.strokeJoin,
        eraseMode: Boolean = this.eraseMode,
        brush: MarkupBrush = this.brush,
        effectStrength: Float = this.effectStrength,
        fillRegion: Boolean = this.fillRegion
    ) = PathProperties(
        strokeWidth, color, alpha, strokeCap, strokeJoin, eraseMode, brush, effectStrength, fillRegion
    )

    fun copyFrom(properties: PathProperties) {
        this.strokeWidth = properties.strokeWidth
        this.color = properties.color
        this.strokeCap = properties.strokeCap
        this.strokeJoin = properties.strokeJoin
        this.eraseMode = properties.eraseMode
        this.brush = properties.brush
        this.effectStrength = properties.effectStrength
        this.fillRegion = properties.fillRegion
    }
}