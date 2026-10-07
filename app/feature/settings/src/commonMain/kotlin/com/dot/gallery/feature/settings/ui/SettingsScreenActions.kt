/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

/**
 * Callbacks the settings hub raises. The wrapper maps them to navigation,
 * preference writes, and the platform support sheet.
 */
class SettingsScreenActions(
    val onNavigate: (SettingsRoute) -> Unit,
    val onDismissBanner: () -> Unit,
    val onRestoreBanner: () -> Unit,
    val onShowSupport: () -> Unit,
    val onDeveloperUnlocked: () -> Unit,
)
