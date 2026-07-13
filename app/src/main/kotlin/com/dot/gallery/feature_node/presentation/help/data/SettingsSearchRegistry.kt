/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.help.data

/**
 * Process-lifetime registry of settings toggles discovered at runtime.
 *
 * Settings sub-screens build their preference lists imperatively inside Compose
 * (titles come from `stringResource`, values from `remember` state hooks), so there
 * is no static list to read. Instead, [com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen]
 * registers the titles it renders here (keyed by the owning screen route) whenever a
 * settings screen is composed. [HelpSearchIndex] then merges these entries so search
 * covers every toggle the user has seen — including ones added in the future — without
 * hand-maintaining a list.
 *
 * The curated [HelpSearchIndex.toggleItems] catalog remains the always-available
 * fallback for screens the user has not opened yet this session.
 */
object SettingsSearchRegistry {

    private val byRoute = LinkedHashMap<String, List<String>>()

    /** Replace the known toggle titles for [route]. Empty titles clear the entry. */
    fun register(route: String, titles: List<String>) {
        if (titles.isEmpty()) {
            byRoute.remove(route)
        } else {
            byRoute[route] = titles
        }
    }

    /** Immutable snapshot of route → toggle titles for index building. */
    fun snapshot(): Map<String, List<String>> = LinkedHashMap(byRoute)
}
