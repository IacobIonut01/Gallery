/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector


/**
 * Runtime data for a shortcut, independent of the persisted layout.
 */
@Immutable
data class RuntimeShortcut(
    val shortcut: LibraryShortcut,
    val title: String,
    val icon: ImageVector?,
    val contentColor: Color,
    val useIndicator: Boolean,
    val indicatorCounter: Int,
    val route: String,
    val available: Boolean
) {
    fun toItem(pref: LibraryShortcutPref) = LibraryShortcutItem(
        shortcut = shortcut,
        title = title,
        icon = icon,
        contentColor = contentColor,
        useIndicator = useIndicator,
        indicatorCounter = indicatorCounter,
        route = route,
        available = available,
        pref = pref
    )
}
