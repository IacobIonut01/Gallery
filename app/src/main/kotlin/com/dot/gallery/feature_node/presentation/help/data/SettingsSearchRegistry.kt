/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.help.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-lifetime registry of settings titles discovered at runtime.
 *
 * [HelpSearchIndex] also has an always-available static catalog for settings that have not been
 * visited. This registry supplements that catalog and is observable, so newly rendered settings
 * become searchable immediately rather than only after the Help screen is recreated.
 */
object SettingsSearchRegistry {

    private val _entries = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val entries: StateFlow<Map<String, List<String>>> = _entries.asStateFlow()

    /** Replace the known setting titles for [route]. Empty titles clear the entry. */
    fun register(route: String, titles: List<String>) {
        val normalized = titles.filter(String::isNotBlank).distinct()
        _entries.update { current ->
            if (normalized.isEmpty()) {
                current - route
            } else {
                current + (route to normalized)
            }
        }
    }

    /** Immutable snapshot of route → setting titles for synchronous index building. */
    fun snapshot(): Map<String, List<String>> = entries.value
}
