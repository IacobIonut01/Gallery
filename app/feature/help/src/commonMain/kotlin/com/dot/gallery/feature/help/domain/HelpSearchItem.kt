/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.domain

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A flattened, resolved search entry. Strings are pre-resolved (not string-res)
 * so the search engine can score them without resource access.
 */
@Immutable
data class HelpSearchItem(
    val kind: HelpSearchKind,
    val id: String,
    val title: String,
    val subtitle: String,
    /** Extra searchable synonyms/keywords, space-joined. */
    val keywords: String,
    val icon: ImageVector?,
    /** Navigation target (null = not directly navigable). */
    val route: HelpRoute?,
) {
    /** Combined text the fuzzy matcher searches over. */
    val searchText: String get() = buildString {
        append(title)
        if (subtitle.isNotEmpty()) append(' ').append(subtitle)
        if (keywords.isNotEmpty()) append(' ').append(keywords)
    }
}
