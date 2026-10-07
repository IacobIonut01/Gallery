/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.components

import androidx.compose.runtime.Composable
import com.dot.gallery.ui.theme.GalleryTheme

/**
 * A stable, preference-independent host for screenshots and interactive
 * previews. The app-side variant additionally provides `LocalEventHandler`.
 */
@Composable
fun PreviewHost(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    GalleryTheme(
        darkTheme = darkTheme,
        dynamicColor = false,
        ignoreUserPreference = true,
        content = content,
    )
}
