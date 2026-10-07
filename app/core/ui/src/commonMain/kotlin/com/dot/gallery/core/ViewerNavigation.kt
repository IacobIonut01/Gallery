/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Route navigation that first gets the media viewer out of the way so the destination is
 * actually revealed. Provided by `MediaViewScreen`: when the viewer is presented as an
 * overlay above the NavHost the destination is pushed underneath and the overlay is then
 * dismissed; when the viewer is a standalone back-stack destination its entry is popped
 * first so it does not linger hidden under the new screen. `null` outside the viewer —
 * read it through [rememberMediaViewerNavigate], which falls back to plain navigation.
 */
val LocalMediaViewerNavigate = compositionLocalOf<((String) -> Unit)?> { null }

enum class ViewerExitStep { Navigate, PopViewer, DismissOverlay }

/**
 * Ordered steps for navigating from inside a media viewer to another destination.
 * Overlay mode pushes the destination underneath the NavHost before dropping the overlay
 * so the push is revealed, never hidden; standalone mode pops the viewer's own back-stack
 * entry first so it does not linger hidden under the new screen.
 */
fun mediaViewerExitSteps(isOverlay: Boolean): List<ViewerExitStep> =
    if (isOverlay) {
        listOf(ViewerExitStep.Navigate, ViewerExitStep.DismissOverlay)
    } else {
        listOf(ViewerExitStep.PopViewer, ViewerExitStep.Navigate)
    }

/**
 * The [LocalMediaViewerNavigate] implementation for a media viewer. [onDismissRequest] is
 * the overlay host's dismiss callback — non-null when the viewer floats above the NavHost
 * (`MediaViewerOverlayHost`), `null` when the viewer is a standalone destination.
 */
@Composable
fun rememberViewerExitNavigate(onDismissRequest: (() -> Unit)?): (String) -> Unit {
    val eventHandler = LocalEventHandler.current
    val latestDismissRequest by rememberUpdatedState(onDismissRequest)
    return remember(eventHandler) {
        { route: String ->
            val dismiss = latestDismissRequest
            mediaViewerExitSteps(isOverlay = dismiss != null).forEach { step ->
                when (step) {
                    ViewerExitStep.Navigate -> eventHandler.navigate(route)
                    ViewerExitStep.PopViewer -> eventHandler.navigateUp()
                    ViewerExitStep.DismissOverlay -> dismiss?.invoke()
                }
            }
        }
    }
}

/**
 * Navigation for composables hosted inside the media viewer (sheets, rows, buttons):
 * closes the viewer when one is open so the destination is revealed, and falls back to
 * plain navigation everywhere else.
 */
@Composable
fun rememberMediaViewerNavigate(): (String) -> Unit {
    val eventHandler = LocalEventHandler.current
    val viewerNavigate = LocalMediaViewerNavigate.current
    return remember(eventHandler, viewerNavigate) {
        viewerNavigate ?: { route: String -> eventHandler.navigate(route) }
    }
}
