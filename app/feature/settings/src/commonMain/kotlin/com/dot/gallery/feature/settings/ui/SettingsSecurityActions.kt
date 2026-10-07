/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Stable

/**
 * Platform actions for [SettingsSecurityContent] — the SAF folder picker and
 * `PrivateFolderManager` calls stay app-side.
 */
@Stable
class SettingsSecurityActions(
    val onPickPrivateFolder: () -> Unit = {},
    val onClearPrivateFolder: () -> Unit = {},
    val onClearPrivateFolderAndReveal: () -> Unit = {},
)
