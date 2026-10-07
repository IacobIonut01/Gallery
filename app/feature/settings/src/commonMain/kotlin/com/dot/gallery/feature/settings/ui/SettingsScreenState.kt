/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

/**
 * UI state for the settings hub. The platform wrapper reads each flag from its
 * settings store and pushes updates down; the content composable never touches
 * preferences directly.
 */
data class SettingsScreenState(
    val developerMode: Boolean,
    val bannerDismissed: Boolean,
    val hasCloudProviders: Boolean,
)
