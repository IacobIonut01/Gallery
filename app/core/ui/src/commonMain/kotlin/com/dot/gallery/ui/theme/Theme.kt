/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

private val LocalGalleryDarkTheme = compositionLocalOf<Boolean?> { null }

/** Returns the effective theme, including explicit app and preview overrides. */
@Composable
fun isDarkTheme(): Boolean {
    LocalGalleryDarkTheme.current?.let { return it }
    val prefs = LocalThemePreferences.current
    val systemDarkTheme = isSystemInDarkTheme()
    return if (prefs.forceTheme) prefs.isDarkMode else systemDarkTheme
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is only supplied by platforms that support it (Android 12+);
    // [platformDynamicColorScheme] returns null everywhere else.
    dynamicColor: Boolean = true,
    ignoreUserPreference: Boolean = false,
    content: @Composable () -> Unit
) {
    val prefs = LocalThemePreferences.current
    val forcedDarkTheme = if (!ignoreUserPreference && prefs.forceTheme) {
        prefs.isDarkMode
    } else {
        darkTheme
    }
    val isAmoledMode = !ignoreUserPreference && prefs.amoledMode
    val themeColorSeed = if (ignoreUserPreference) {
        THEME_SEED_SYSTEM
    } else {
        prefs.themeColorSeed
    }
    val useSystemFont = !ignoreUserPreference && prefs.useSystemFont
    val dynamicScheme = if (dynamicColor) {
        platformDynamicColorScheme(forcedDarkTheme)
    } else {
        null
    }
    val colorScheme = remember(dynamicScheme, forcedDarkTheme, isAmoledMode, themeColorSeed) {
        when {
            themeColorSeed == THEME_SEED_NEUTRAL ->
                neutralColorScheme(forcedDarkTheme, isAmoledMode)

            themeColorSeed != THEME_SEED_SYSTEM -> {
                val seedArgb = themeColorSeed.toLongOrNull(16)?.toInt() ?: seed.value.toInt()
                colorSchemeFromSeed(seedArgb, forcedDarkTheme, isAmoledMode)
            }

            dynamicScheme != null -> dynamicScheme.maybeAmoled(isAmoledMode && forcedDarkTheme)

            else ->
                if (forcedDarkTheme) DarkColors.maybeAmoled(isAmoledMode) else LightColors
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        typography = if (useSystemFont) SystemTypography else Typography
    ) {
        CompositionLocalProvider(
            LocalGalleryDarkTheme provides forcedDarkTheme,
            LocalOverscrollFactory provides null,
            content = content
        )
    }
}
