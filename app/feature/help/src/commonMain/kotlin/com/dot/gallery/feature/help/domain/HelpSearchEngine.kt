/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.domain

/**
 * Ranks [items] against [query], best-first. Injected because the production
 * scorer (`HelpFuzzyMatcher`, fuzzywuzzy) is JVM/Android-only; the module ships
 * [BasicHelpSearchEngine] as the portable default.
 */
fun interface HelpSearchEngine {
    fun search(items: List<HelpSearchItem>, query: String, limit: Int): List<HelpSearchItem>
}

/** Convenience default limit — `fun interface` methods can't have default args. */
fun HelpSearchEngine.search(
    items: List<HelpSearchItem>,
    query: String,
): List<HelpSearchItem> = search(items, query, limit = 25)
