/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import com.dot.gallery.core.ui.util.AppBuildInfo
import com.dot.gallery.core.ui.util.LocalAppBuildInfo
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.ui.theme.GalleryTheme
import com.dot.gallery.ui.theme.LocalThemePreferences
import com.dot.gallery.ui.theme.ThemePreferences
import com.dot.gallery.BuildConfig
import com.dot.gallery.core.ui.settings.rememberPreference

/**
 * Feeds the platform-owned locals that the shared `GalleryTheme` reads —
 * theme preferences read through `LocalSettingsStore` (the app's encrypted
 * DataStore on Android, localStorage on wasmJs), the build identity from
 * `BuildConfig`. Must wrap every composable root.
 */
@Composable
fun AppThemeLocals(
    content: @Composable () -> Unit,
) {
    val forceTheme by rememberPreference(SettingsKeys.forceTheme)
    val isDarkMode by rememberPreference(SettingsKeys.darkMode)
    val amoledMode by rememberPreference(SettingsKeys.amoledMode)
    val themeColorSeed by rememberPreference(SettingsKeys.themeColorSeed)
    val useSystemFont by rememberPreference(SettingsKeys.useSystemFont)
    CompositionLocalProvider(
        LocalThemePreferences provides ThemePreferences(
            forceTheme = forceTheme,
            isDarkMode = isDarkMode,
            amoledMode = amoledMode,
            themeColorSeed = themeColorSeed,
            useSystemFont = useSystemFont,
        ),
        LocalAppBuildInfo provides AppBuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
        ),
        content = content,
    )
}
