/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Stable

/**
 * `onRequestExcludeVideos` runs the membership-count check app-side; the
 * wrapper either writes the pref directly (count == 0) or sets
 * [CategoriesSettingsState.videoPurgeCount] to show the purge dialog.
 */
@Stable
class CategoriesSettingsActions(
    val onStartScan: () -> Unit = {},
    val onStopScan: () -> Unit = {},
    val onResetCategories: () -> Unit = {},
    val onRequestExcludeVideos: () -> Unit = {},
    val onConfirmVideoPurge: () -> Unit = {},
    val onDismissVideoPurge: () -> Unit = {},
)
