package com.dot.gallery.feature_node.presentation.common.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs

@Composable
fun maxScrollFlingBehavior(): FlingBehavior {
    val flingSpec = rememberSplineBasedDecay<Float>()
    return remember(flingSpec) {
        ScrollSpeedFlingBehavior(flingSpec)
    }
}

private class ScrollSpeedFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val maxVelocity = 20_000F
        val newVelocity =
            if (initialVelocity > 0F) minOf(initialVelocity, maxVelocity)
            else maxOf(initialVelocity, -maxVelocity)

        return if (abs(newVelocity) > 1f) {
            var velocityLeft = newVelocity
            var lastValue = 0f
            AnimationState(
                initialValue = 0f,
                initialVelocity = newVelocity,
            ).animateDecay(flingDecay) {
                val delta = value - lastValue
                val consumed = scrollBy(delta)
                lastValue = value
                velocityLeft = this.velocity
                // avoid rounding errors and stop if anything is unconsumed
                if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
            }
            velocityLeft
        } else newVelocity
    }
}