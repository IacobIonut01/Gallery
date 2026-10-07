/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.settings.subsettings

import androidx.compose.runtime.Composable
import com.dot.gallery.feature.settings.ui.SlideshowSettingsContent
import com.dot.gallery.feature.help.domain.SettingsSearchRegistry
import com.dot.gallery.core.presentation.util.Screen

@Composable
fun SlideshowSettingsScreen() {
    SlideshowSettingsContent(
        searchRoute = Screen.SlideshowSettingsScreen(),
        searchSink = SettingsSearchRegistry::register,
    )
}
