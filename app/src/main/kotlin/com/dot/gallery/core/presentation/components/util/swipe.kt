package com.dot.gallery.core.presentation.components.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.dot.gallery.feature_node.presentation.util.rememberFeedbackManager
import kotlin.math.roundToInt

@Composable
fun Modifier.swipe(
    enabled: Boolean = true,
    onSwipeDown: () -> Unit,
    onSwipeUp: (() -> Unit)?
): Modifier {
    var delta by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val feedbackManager = rememberFeedbackManager()
    var isVibrating by remember { mutableStateOf(false) }
    val draggableState = rememberDraggableState {
        delta += if (onSwipeUp != null) it else if (it > 0) it else 0f
        delta = delta.coerceIn(-185f, 400f)
        if (!isVibrating && (delta == 400f || (onSwipeUp != null && delta == -185f))) {
            feedbackManager.vibrate()
            isVibrating = true
        }
    }
    val animatedDelta by animateFloatAsState(
        label = "animatedDelta",
        targetValue = if (isDragging) delta else 0f,
        animationSpec = tween(
            durationMillis = 200
        )
    )
    return this then Modifier
        .draggable(
            enabled = enabled,
            state = draggableState,
            orientation = Orientation.Vertical,
            onDragStarted = {
                isVibrating = false
                isDragging = true
            },
            onDragStopped = {
                isVibrating = false
                isDragging = false
                if (delta == 400f) {
                    onSwipeDown()
                }
                if (onSwipeUp != null && delta == -185f) {
                    onSwipeUp()
                }
                delta = 0f
            }
        )
        .offset {
            IntOffset(0, if (isDragging) delta.roundToInt() else animatedDelta.roundToInt())
        }
}