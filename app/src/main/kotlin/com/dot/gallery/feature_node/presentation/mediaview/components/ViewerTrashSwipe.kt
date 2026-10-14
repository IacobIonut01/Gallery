/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Upward drag distance (px) that commits the move-to-trash gesture on release. */
const val SWIPE_UP_TRASH_THRESHOLD = 200f

/**
 * Progress (0..1) of the swipe-up-to-trash gesture for an upward drag of [dragUpDistance] px.
 * Reaching 1 means a release now moves the media to the trash.
 */
fun swipeUpTrashProgress(dragUpDistance: Float): Float =
    (dragUpDistance / SWIPE_UP_TRASH_THRESHOLD).coerceIn(0f, 1f)

/**
 * Trash target shown while the swipe-up gesture is in flight. It fades and grows with [progress]
 * and switches to the error colors once a release would move the media to the trash.
 *
 * Renders nothing at rest, so the viewer pays nothing while the gesture is idle.
 */
@Composable
fun TrashSwipeIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        // Track the finger exactly while dragging; only the release (back to 0) is animated.
        animationSpec = if (progress <= 0f) spring() else snap(),
        label = "TrashSwipeProgress",
    )
    if (animatedProgress <= 0f) return
    val pastThreshold = progress >= 1f
    Box(
        modifier = modifier
            .graphicsLayer { alpha = animatedProgress }
            .scale(0.6f + 0.4f * animatedProgress)
            .size(72.dp)
            .background(
                color = if (pastThreshold) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
                },
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            // Decorative: the outcome is announced by the viewer's snackbar.
            contentDescription = null,
            tint = if (pastThreshold) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                Color.White
            },
            modifier = Modifier.size(32.dp),
        )
    }
}
