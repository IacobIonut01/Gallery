package com.dot.gallery.feature_node.presentation.mediaview.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

@Composable
fun TrackVisibility(
    modifier: Modifier = Modifier,
    onVisibilityChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var position by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                size = coordinates.size
                position = coordinates.positionInWindow()
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    val isVisible = position.x >= 0 && position.y >= 0 &&
                            position.x + size.width <= with(density) { constraints.maxWidth.toDp().toPx() } &&
                            position.y + size.height <= with(density) { constraints.maxHeight.toDp().toPx() }
                    onVisibilityChanged(isVisible)
                    placeable.place(0, 0)
                }
            }
    ) {
        content()
    }
}