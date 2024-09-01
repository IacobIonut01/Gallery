package com.dot.gallery.feature_node.presentation.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Stable
fun Int.roundDpToPx(density: Density) = with(density) { dp.roundToPx() }

@Stable
fun Int.roundSpToPx(density: Density) = with(density) { sp.roundToPx() }

fun Float.normalize(
    minValue: Float,
    maxValue: Float = 1f,
    minNormalizedValue: Float = 0f,
    maxNormalizedValue: Float = 1f
): Float {
    return ((this - minValue) / (maxValue - minValue)).coerceIn(
        minNormalizedValue,
        maxNormalizedValue
    )
}

