/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The surface the media-viewer overlay exposes to grid cells: whether the viewer is
 * presented and which media id it currently shows, so a committed dismiss flight can
 * preposition the target cell. The concrete controller lives app-side and exposes
 * this via [LocalMediaViewerOverlayController].
 */
interface MediaViewerOverlaySignals {
    val visible: Boolean
    val currentMediaId: Long
}

val LocalMediaViewerOverlayController = staticCompositionLocalOf<MediaViewerOverlaySignals?> { null }
