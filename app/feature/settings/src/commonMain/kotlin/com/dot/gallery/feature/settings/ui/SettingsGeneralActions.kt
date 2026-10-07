/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

data class SettingsGeneralActions(
    val onOpenTrash: () -> Unit = {},
    /**
     * Applies a launcher alias pair. The wrapper performs the platform work
     * (`changeAppAlias` + delayed restart) in addition to storing the prefs.
     */
    val onAppAliasChange: (name: String, logo: String) -> Unit = { _, _ -> },
    /** Wired to `SettingsSearchRegistry::register` in the app. */
    val searchSink: ((String, List<String>) -> Unit)? = null,
    val searchRoute: String? = null,
)
