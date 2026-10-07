/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.dot.gallery.domain.model.Media

/**
 * What the overlay host draws in place of the media viewer while it exits: the scrim the
 * viewer last rendered (with its drag-faded alpha) and the media thumbnail — null when a
 * return flight already owns the media's visual. The interactive viewer leaves composition
 * with the exit so its gesture surfaces can't swallow a fast re-tap aimed at the grid
 * underneath; this stand-in keeps the fade visuals without any pointer-input nodes.
 */
@Stable
class ViewerExitVisual(
    val scrim: Color,
    val media: Media?,
)
