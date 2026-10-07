/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui


/**
 * Lazy-grid item keys in display order — used to resolve a persisted
 * [LibraryScrollPosition] back into an index for `rememberLazyGridState`.
 */
fun libraryGridSectionKeys(
    hasLocations: Boolean,
    hasLocationsList: Boolean,
    hasPeople: Boolean,
    hasPeopleList: Boolean,
    hasCategories: Boolean,
    hasNoCategories: Boolean,
): List<String> = buildList {
    add("libraryShortcuts")
    if (hasLocations) add("LocationsHeader")
    if (hasLocationsList) add("LocationsList")
    if (hasPeople) add("PeopleHeader")
    if (hasPeopleList) add("PeopleList")
    if (hasCategories) {
        add("CategoriesHeader")
        add("CategoriesList")
    }
    if (hasNoCategories) add("NoCategories")
}
