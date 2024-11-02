package com.dot.gallery.feature_node.presentation.edit.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.dot.gallery.feature_node.domain.model.editor.PainterMotionEvent

suspend fun AwaitPointerEventScope.awaitDragMotionEvent(
    onTouchEvent: (PainterMotionEvent, PointerInputChange) -> Unit
) {
    // Wait for at least one pointer to press down, and set first contact position
    val down: PointerInputChange = awaitFirstDown()
    onTouchEvent(PainterMotionEvent.Down, down)

    var pointer = down

    // Waits for drag threshold to be passed by pointer
    // or it returns null if up event is triggered
    val change: PointerInputChange? =
        awaitTouchSlopOrCancellation(down.id) { change: PointerInputChange, over: Offset ->
            // If consumePositionChange() is not consumed drag does not
            // function properly.
            // Consuming position change causes change.positionChanged() to return false.
            if (change.positionChange() != Offset.Zero) change.consume()
        }

    if (change != null) {
        // Calls  awaitDragOrCancellation(pointer) in a while loop
        drag(change.id) { pointerInputChange: PointerInputChange ->
            pointer = pointerInputChange
            onTouchEvent(PainterMotionEvent.Move, pointer)
        }

        // All of the pointers are up
        onTouchEvent(PainterMotionEvent.Up, pointer)
    } else {
        // Drag threshold is not passed and last pointer is up
        onTouchEvent(PainterMotionEvent.Up, pointer)
    }
}

fun Modifier.dragMotionEvent(onTouchEvent: (PainterMotionEvent, PointerInputChange) -> Unit) = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitDragMotionEvent(onTouchEvent)
        }
    }
)

suspend fun AwaitPointerEventScope.awaitDragMotionEvent(
    onDragStart: (PointerInputChange) -> Unit = {},
    onDrag: (PointerInputChange) -> Unit = {},
    onDragEnd: (PointerInputChange) -> Unit = {}
) {
    // Wait for at least one pointer to press down, and set first contact position
    val down: PointerInputChange = awaitFirstDown()
    onDragStart(down)

    var pointer = down

    // Waits for drag threshold to be passed by pointer
    // or it returns null if up event is triggered
    val change: PointerInputChange? =
        awaitTouchSlopOrCancellation(down.id) { change: PointerInputChange, over: Offset ->
            // If consumePositionChange() is not consumed drag does not
            // function properly.
            // Consuming position change causes change.positionChanged() to return false.
            if (change.positionChange() != Offset.Zero) change.consume()
        }

    if (change != null) {
        // Calls  awaitDragOrCancellation(pointer) in a while loop
        drag(change.id) { pointerInputChange: PointerInputChange ->
            pointer = pointerInputChange
            onDrag(pointer)
        }

        // All of the pointers are up
        onDragEnd(pointer)
    } else {
        // Drag threshold is not passed and last pointer is up
        onDragEnd(pointer)
    }
}

fun Modifier.dragMotionEvent(
    key: Any? = Unit,
    onDragStart: (PointerInputChange) -> Unit = {},
    onDrag: (PointerInputChange) -> Unit = {},
    onDragEnd: (PointerInputChange) -> Unit = {}
) = this.then(
    Modifier.pointerInput(key) {
        awaitEachGesture {
            awaitDragMotionEvent(onDragStart, onDrag, onDragEnd)
        }
    }
)