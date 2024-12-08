package com.dot.gallery.feature_node.presentation.library.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a dashed border around a Composable component.
 *
 * @param color The color of the dashed border.
 * @param shape The shape of the dashed border.
 * @param strokeWidth The width of the dashed border stroke.
 * @param dashLength The length of each dash in the border.
 * @param gapLength The length of the gap between each dash.
 * @param cap The style of the stroke caps at the ends of dashes.
 *
 * @return A Modifier with the dashed border applied.
 */
fun Modifier.dashedBorder(
    color: Color,
    shape: Shape,
    strokeWidth: Dp = 2.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
    cap: StrokeCap = StrokeCap.Round
) = dashedBorder(brush = SolidColor(color), shape, strokeWidth, dashLength, gapLength, cap)

/**
 * Adds a dashed border around a Composable component.
 *
 * @param brush The brush of the dashed border.
 * @param shape The shape of the dashed border.
 * @param strokeWidth The width of the dashed border stroke.
 * @param dashLength The length of each dash in the border.
 * @param gapLength The length of the gap between each dash.
 * @param cap The style of the stroke caps at the ends of dashes.
 *
 * @return A Modifier with the dashed border applied.
 */
fun Modifier.dashedBorder(
    brush: Brush,
    shape: Shape,
    strokeWidth: Dp = 2.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
    cap: StrokeCap = StrokeCap.Round
) = this.drawWithContent {

    val outline = shape.createOutline(size, layoutDirection, density = this)

    val dashedStroke = Stroke(
        cap = cap,
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx())
        )
    )

    drawContent()

    drawOutline(
        outline = outline,
        style = dashedStroke,
        brush = brush
    )
}