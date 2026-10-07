/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui.search

import androidx.compose.ui.graphics.vector.ImageVector
import com.dot.gallery.core.SettingsEntity


/**
 * Represents a search suggestion that can be displayed in the [SearchContent].
 *
 * This system is designed to be easily extensible: to add a new suggestion type,
 * simply create a new [SearchSuggestionProvider] implementation and register it
 * in the providers list handed to the screen.
 */
data class SearchSuggestion(
    val title: String,
    val icon: ImageVector,
    val action: (SearchActions) -> Unit
)

/**
 * Converts a list of [SearchSuggestionProvider]s into a flat list of [SettingsEntity]
 * items suitable for rendering via `SettingsOptionLayout`,
 * wiring each suggestion's action to the given [actions].
 */
fun List<SearchSuggestionProvider>.toSettingsEntities(
    actions: SearchActions
): List<SettingsEntity> {
    return flatMap { provider ->
        if (provider.suggestions.isEmpty()) return@flatMap emptyList()
        listOf(SettingsEntity.Header(provider.headerTitle)) +
            provider.suggestions.map { suggestion ->
                SettingsEntity.Preference(
                    title = suggestion.title,
                    icon = suggestion.icon,
                    onClick = { suggestion.action(actions) }
                )
            }
    }
}
