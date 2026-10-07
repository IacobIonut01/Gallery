package com.dot.gallery.core.presentation.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt



/**
 * Snaps a blur radius to fixed [step]-sized buckets.
 *
 * The GPU compiles a distinct shader/pipeline for every distinct blur radius. Feeding a
 * continuously animated or slider-driven radius into [Modifier.blur] (or a [android.graphics.RenderEffect]
 * blur) therefore spawns a brand-new pipeline on nearly every frame, which overflows the
 * platform HWUI shader cache and spams logcat with
 * `ShaderCache::store: sizes ... not allowed`.
 *
 * Quantizing the radius collapses those hundreds of unique radii down to a handful, so the
 * same compiled shader is reused and the cache stays small enough to persist.
 */
fun Dp.quantizeBlur(step: Dp = 4.dp): Dp {
    if (value <= 0f) return 0.dp
    val s = step.value.coerceAtLeast(1f)
    return ((value / s).roundToInt() * s).dp
}

/** [Float] (pixel/dp) radius variant of [quantizeBlur]. */
fun Float.quantizeBlur(step: Float = 4f): Float {
    if (this <= 0f) return 0f
    val s = step.coerceAtLeast(1f)
    return (this / s).roundToInt() * s
}

/**
 * [Modifier.blur] with the radius snapped to [step]-sized buckets to limit shader-pipeline churn.
 * No blur is applied when the quantized radius is zero.
 */
fun Modifier.stableBlur(radius: Dp, step: Dp = 4.dp): Modifier {
    val quantized = radius.quantizeBlur(step)
    return if (quantized > 0.dp) this.blur(quantized) else this
}
