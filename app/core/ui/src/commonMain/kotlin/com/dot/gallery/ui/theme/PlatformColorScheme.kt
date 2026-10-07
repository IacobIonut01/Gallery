/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Resolves the platform-provided (dynamic) color scheme, or `null` when the
 * platform cannot supply one — Android 12+ Material You on Android, always
 * `null` elsewhere. Callers fall back to the static seed palettes.
 */
@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?
