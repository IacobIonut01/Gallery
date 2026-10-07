/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.util

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/** App version identity, provided above each composable root. */
@Immutable
data class AppBuildInfo(
    val versionName: String = "0.0.0",
    val versionCode: Long = 0,
)

val LocalAppBuildInfo = compositionLocalOf { AppBuildInfo() }
