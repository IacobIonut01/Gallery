/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.ui

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Whether the platform requests reduced motion (Android: animator duration
 * scale == 0). Provided by the app wrapper; preview animations pause on the
 * first frame when true.
 */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * Reusable animation state machine for help preview composables.
 *
 * Cycles through [stepCount] steps, pausing between each,
 * with smooth progress transitions.
 *
 * Respects [LocalReduceMotion] — when enabled, stays on step 0.
 */
@Composable
fun rememberPreviewAnimation(
    stepCount: Int,
    stepDurationMs: Long = PreviewConstants.DEFAULT_STEP_DURATION_MS,
    pauseDurationMs: Long = PreviewConstants.DEFAULT_PAUSE_DURATION_MS,
    reduceMotion: Boolean = LocalReduceMotion.current,
): PreviewAnimationState {
    val state = remember(stepCount) { PreviewAnimationState(stepCount) }

    if (!reduceMotion && stepCount > 1) {
        LaunchedEffect(stepCount) {
            while (true) {
                delay(pauseDurationMs)
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = stepDurationMs.toInt(),
                        easing = EaseInOutCubic
                    )
                ) { value, _ ->
                    state.stepProgress = value
                }
                state.currentStep = (state.currentStep + 1) % stepCount
                state.stepProgress = 0f
            }
        }
    }

    return state
}

/**
 * A layout wrapper that gives its child extra space (so lazy lists/grids
 * compose more items) and then translates the rendered content based on
 * [scrollProgress] (0 → 1). The parent must have `clipToBounds()` to
 * hide the overflow, creating a real scroll effect.
 *
 * @param scrollProgress Normalized progress (0 = top, 1 = scrolled by [scrollDistanceDp]).
 * @param scrollDistanceDp How far the content scrolls.
 * @param horizontal If true, scrolls horizontally instead of vertically.
 */
@Composable
fun AutoScrollBox(
    scrollProgress: Float,
    modifier: Modifier = Modifier,
    scrollDistanceDp: Dp = 120.dp,
    horizontal: Boolean = false,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val extraPx = with(density) { scrollDistanceDp.roundToPx() }
    val offsetPx = -extraPx * scrollProgress

    Layout(
        content = content,
        modifier = modifier.graphicsLayer {
            if (horizontal) translationX = offsetPx else translationY = offsetPx
        }
    ) { measurables, constraints ->
        val childConstraints = if (horizontal) {
            constraints.copy(maxWidth = constraints.maxWidth + extraPx)
        } else {
            constraints.copy(maxHeight = constraints.maxHeight + extraPx)
        }
        val placeables = measurables.map { it.measure(childConstraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}
