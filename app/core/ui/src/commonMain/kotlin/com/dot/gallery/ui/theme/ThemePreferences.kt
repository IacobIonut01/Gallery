/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/** Resolved user theming choices, provided above each composable root. */
@Immutable
data class ThemePreferences(
    val forceTheme: Boolean = false,
    val isDarkMode: Boolean = false,
    val amoledMode: Boolean = false,
    val themeColorSeed: String = THEME_SEED_SYSTEM,
    val useSystemFont: Boolean = false,
)

val LocalThemePreferences = compositionLocalOf { ThemePreferences() }
