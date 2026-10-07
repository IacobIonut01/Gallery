/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.dot.gallery.domain.model.Media

/**
 * A manually driven shared-element flight. [bounds] is the live morph rect in root space and
 * [progress] (0->1) drives the thumbnail's content-scale interpolation so the flight lands
 * exactly on the destination's rendering.
 *
 * For a committed dismiss ([isEnter] = false) bounds animate release->cell with Fit->Crop. For
 * an overlay open ([isEnter] = true) bounds animate cell->fullscreen with Crop->Fit — the
 * framework's own bounds morph can't be trusted for the deferred enter (its DeferredAnimation
 * is recreated once the transition's currentState already reads the target, so it initializes
 * at the end bounds and the media pops in instead of morphing).
 */
@Stable
class ViewerDismissFlight(
    val key: Any,
    val media: Media,
    bounds: Rect,
    val isEnter: Boolean = false,
) {
    var bounds by mutableStateOf(bounds)
        set

    var progress by mutableFloatStateOf(0f)
        set
}
