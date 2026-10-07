/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import androidx.compose.runtime.compositionLocalOf
import com.dot.gallery.domain.model.UIEvent
import com.dot.gallery.domain.util.EventHandler

val LocalEventHandler = compositionLocalOf<EventHandler> {
    error("No EventHandler provided!!! This is likely due to a missing inject binding in the Composable hierarchy.")
}

fun EventHandler.navigateUp() = pushEvent(UIEvent.NavigationUpEvent)
fun EventHandler.navigate(route: String) = pushEvent(UIEvent.NavigationRouteEvent(route))
fun EventHandler.toggleNavigationBar(show: Boolean) = pushEvent(UIEvent.ToggleNavigationBarEvent(show))
fun EventHandler.setFollowTheme(followTheme: Boolean) = pushEvent(UIEvent.SetFollowThemeEvent(followTheme))
