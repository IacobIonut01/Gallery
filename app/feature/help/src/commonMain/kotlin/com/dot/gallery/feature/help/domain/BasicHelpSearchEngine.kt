/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.domain

/**
 * Portable [HelpSearchEngine] fallback: substring matching over the item's
 * searchable text with the same field-weighting and kind priority the
 * fuzzywuzzy-backed Android engine applies on top of its raw score.
 *
 * An empty query yields an empty list (callers decide what to show for the
 * empty state).
 */
object BasicHelpSearchEngine : HelpSearchEngine {

    override fun search(
        items: List<HelpSearchItem>,
        query: String,
        limit: Int
    ): List<HelpSearchItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty() || items.isEmpty()) return emptyList()

        return items
            .filter { it.searchText.lowercase().contains(q) }
            .map { it to boostedScore(it, q) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Field-weighting and kind priority so exact/prefix title matches outrank
     * generic body matches — mirrors `HelpFuzzyMatcher.boostedScore`.
     */
    private fun boostedScore(item: HelpSearchItem, query: String): Int {
        val title = item.title.lowercase()
        var score = 0

        when {
            title == query -> score += 70
            title.startsWith(query) -> score += 45
            title.contains(query) -> score += 28
        }
        if (item.subtitle.lowercase().contains(query)) score += 12
        if (item.keywords.lowercase().contains(query)) score += 10

        score += when (item.kind) {
            HelpSearchKind.TIP -> 6
            HelpSearchKind.SETTING -> 5
            HelpSearchKind.QUICK_ACTION -> 4
            HelpSearchKind.CHANGELOG -> 0
        }
        return score
    }
}
