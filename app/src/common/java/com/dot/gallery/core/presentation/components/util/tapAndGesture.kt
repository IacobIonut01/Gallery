/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.core.presentation.components.util

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.debugInspectorInfo
import com.smarttoolfactory.gesture.detectTransformGestures
import kotlinx.coroutines.launch

/**
 * Extension of [Modifier] to support both
 * taps and gesture operation without overlapping each other in
 * a scrollable container like HorizontalPager
 * @param key Specifying the captured value as a key parameter will
 * cause block to cancel and restart from the beginning if the value changes
 * @param onTap same with clickable, single tap action
 * @param onDoubleTap same with onDoubleTap from combinedClickable, double tap action
 * @param onGesture Received centeroid (Offset), pan (Offset), zoom (Float) and rotation (Float)
 * @param scrollEnabled MutableState<of Boolean> is used to check if it's ok to consume the gestures,
 * in case of a HorizontalPager if we consume all gesture, then the swiping gesture of the Pager will
 * also be ignored; on the other hand, if we don't consume any gesture fixes the previous issue, but a
 * new one is found: both transform and tap gestures are overlapping, which gives a pretty bad UX in a
 * case when the user wants to zoom then pan an image, the tap/double tap gesture is also registered
 *
 * !(scrollEnabled logic value changes should be implemented manually for each individual case)
 */

fun Modifier.tapAndGesture(
    key: Any? = Unit,
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onGesture: ((centeroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit)? = null,
    scrollEnabled: MutableState<Boolean> = mutableStateOf(false)
) = composed(
    factory = {
        val scope = rememberCoroutineScope()

        val gestureModifier = Modifier.pointerInput(key) {
            /**
             * [detectTransformGestures]
             * @author SmartToolFactory
             * href: https://github.com/SmartToolFactory/Compose-Extended-Gestures
             */
            detectTransformGestures(
                consume = false,
                onGesture = { centroid: Offset,
                              pan: Offset,
                              zoom: Float,
                              rotation: Float,
                              _: PointerInputChange,
                              changes: List<PointerInputChange> ->
                    scope.launch {
                        onGesture?.invoke(centroid, pan, zoom, rotation)
                    }
                    changes.forEach {
                        // Consume if scroll gestures are not possible
                        if (!scrollEnabled.value) it.consume()
                    }
                }
            )
        }
        val tapModifier = Modifier.pointerInput(key) {
            detectTapGestures(
                onDoubleTap = onDoubleTap,
                onTap = onTap
            )
        }
        then(gestureModifier.then(tapModifier))
    },
    inspectorInfo = debugInspectorInfo {
        name = "tapAndGesture"
        properties["key"] = key
        properties["onTap"] = onTap
        properties["onDoubleTap"] = onDoubleTap
        properties["onGesture"] = onGesture
    }
)