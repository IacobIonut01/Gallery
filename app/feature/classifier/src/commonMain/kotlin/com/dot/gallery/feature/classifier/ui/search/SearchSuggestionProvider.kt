/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui.search


/**
 * Provides a group of search suggestions with a header title.
 * Implement this interface to add new suggestion categories
 * (e.g. locations, mime types, media tags, albums, etc.)
 */
interface SearchSuggestionProvider {
    /** Header title shown above this group of suggestions */
    val headerTitle: String

    /** The list of suggestions in this group. Empty list means this group is hidden. */
    val suggestions: List<SearchSuggestion>
}
