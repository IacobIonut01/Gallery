package com.dot.gallery.feature_node.presentation.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.verticalFadingEdge(percentage: Float) = this.fadingEdge(
    Brush.verticalGradient(
        0f to Color.Transparent,
        percentage to Color.Red,
        1f - percentage to Color.Red,
        1f to Color.Transparent
    )
)

fun Modifier.horizontalFadingEdge(percentage: Float) = this.fadingEdge(
    Brush.horizontalGradient(
        0f to Color.Transparent,
        percentage to Color.Red,
        1f - percentage to Color.Red,
        1f to Color.Transparent
    )
)

fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }