/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp


/**
 * Animated, blurred radial-gradient background made of three slowly drifting colored
 * "circles", mirroring the welcome experience used elsewhere. Place it at the bottom of a
 * [androidx.compose.foundation.layout.Box] so wizard content draws on top.
 */
@Composable
fun SetupAnimatedBackground(modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer

    val transition = rememberInfiniteTransition(label = "setup-bg")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "setup-bg-fraction"
    )

    val scrim = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(color = surface)
                .blur(200.dp)
                .drawWithCache {
                    val cx = size.width - size.width * fraction
                    val cy = size.height * fraction

                    onDrawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(secondaryContainer, secondaryContainer),
                                center = Offset(cx, cy),
                                tileMode = TileMode.Decal,
                                radius = size.width
                            )
                        )
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryContainer, primaryContainer),
                                center = Offset(cx - cy + cy * 1.2f, cx * 1.2f),
                                tileMode = TileMode.Decal,
                                radius = size.width / 1.5f
                            )
                        )
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(tertiaryContainer, tertiaryContainer),
                                center = Offset(cx - cy + cy / 1.2f, cy + cx - cx / 1.2f),
                                tileMode = TileMode.Decal,
                                radius = size.width / 1.5f
                            )
                        )
                    }
                }
        )

        // Scrim to improve readability/contrast of content drawn on top of the
        // animated, colorful background. Darkest at the top and bottom (where the
        // title and action buttons sit), lighter through the middle.
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to scrim.copy(alpha = 0.80f),
                            0.25f to scrim.copy(alpha = 0.55f),
                            0.5f to scrim.copy(alpha = 0.40f),
                            0.75f to scrim.copy(alpha = 0.55f),
                            1.0f to scrim.copy(alpha = 0.85f)
                        )
                    )
                    onDrawBehind { drawRect(brush = brush) }
                }
        )
    }
}
