package com.dot.gallery.feature_node.presentation.util

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

val LocalHazeState = compositionLocalOf { HazeState() }

/**
 * [hazeEffect] variant that downsamples the blur input via [HazeInputScale.Auto].
 *
 * The frosted-glass output is visually identical (the result is already heavily blurred),
 * but the blur shader processes ~9x fewer pixels for large radii, which removes most of the
 * per-frame cost when many haze layers are drawn at once (e.g. the media view bottom sheet).
 * [HazeInputScale.Auto] automatically disables scaling for small blur radii (< 7.dp), so thin
 * materials stay crisp.
 */
@OptIn(ExperimentalHazeApi::class)
@Stable
fun Modifier.hazeEffectScaled(
    state: HazeState,
    style: HazeStyle,
): Modifier = hazeEffect(state = state, style = style) {
    inputScale = HazeInputScale.Auto
}
