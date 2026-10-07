/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.ui.settings.LocalSettingsStore
import com.dot.gallery.domain.settings.MapSettingsStore
import com.dot.gallery.core.ui.util.AppBuildInfo
import com.dot.gallery.core.ui.util.LocalAppBuildInfo
import com.dot.gallery.docs.DocsEventHandler
import com.dot.gallery.docs.DocsScene
import com.dot.gallery.ui.theme.GalleryTheme
import com.dot.gallery.ui.theme.LocalThemePreferences
import com.dot.gallery.ui.theme.ThemePreferences

/**
 * Mirrors the composition scaffolding the wasmJs `LiveSceneHost` installs —
 * theme prefs, build info, settings store, no-op event handler — so a scene
 * renders identically under `captureToImage` and in the browser iframe.
 */
@Composable
fun DocsSceneHost(theme: String?, scene: DocsScene, state: String?) {
    val prefs = when (theme) {
        "light" -> ThemePreferences(forceTheme = true, isDarkMode = false)
        "dark" -> ThemePreferences(forceTheme = true, isDarkMode = true)
        "amoled" -> ThemePreferences(forceTheme = true, isDarkMode = true, amoledMode = true)
        else -> ThemePreferences()
    }
    CompositionLocalProvider(
        LocalThemePreferences provides prefs,
        LocalAppBuildInfo provides AppBuildInfo(versionName = "1.9.0", versionCode = 190),
        LocalSettingsStore provides remember { MapSettingsStore() },
        LocalEventHandler provides remember { DocsEventHandler() },
    ) {
        GalleryTheme(dynamicColor = false) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                scene.content(state)
            }
        }
    }
}
