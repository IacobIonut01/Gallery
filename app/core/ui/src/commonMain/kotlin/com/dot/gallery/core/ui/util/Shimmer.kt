/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Draws a light sweep over already-drawn content — the shared skeleton-loading
 * shimmer (the app's `com.valentinilk.shimmer` dependency is Android-only).
 */
fun Modifier.shimmerLoading(
    durationMillis: Int = 1400,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerLoading")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerLoadingProgress",
    )
    drawWithContent {
        drawContent()
        val sweepWidth = size.width * 0.6f
        val x = -sweepWidth + (size.width + sweepWidth * 2f) * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                start = Offset(x, 0f),
                end = Offset(x + sweepWidth, size.height),
            )
        )
    }
}
