/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

data class SettingsNavigationActions(
    val onOpenSelectionActions: () -> Unit = {},
    /** Wired to `SettingsSearchRegistry::register` in the app. */
    val searchSink: ((String, List<String>) -> Unit)? = null,
    val searchRoute: String? = null,
)
