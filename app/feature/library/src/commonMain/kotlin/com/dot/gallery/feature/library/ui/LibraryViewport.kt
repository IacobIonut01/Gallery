/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui

import kotlinx.serialization.Serializable


@Serializable
data class LibraryViewport(
    val grid: LibraryScrollPosition = LibraryScrollPosition(),
    val locations: LibraryScrollPosition = LibraryScrollPosition(),
    val people: LibraryScrollPosition = LibraryScrollPosition(),
    val categories: LibraryScrollPosition = LibraryScrollPosition(),
    val configuration: String = "",
    val statusBarTop: Int? = null,
    val navigationBarBottom: Int? = null
)

fun mergeLibraryViewport(
    current: LibraryViewport,
    incoming: LibraryViewport
): LibraryViewport {
    val configChanged = incoming.configuration.isNotBlank() &&
        incoming.configuration != current.configuration
    fun retained(row: LibraryScrollPosition): LibraryScrollPosition =
        if (configChanged) row.copy(offset = 0) else row
    return LibraryViewport(
        grid = if (incoming.grid.isMeasuredPosition()) incoming.grid else retained(current.grid),
        locations = if (incoming.locations.isMeasuredPosition()) {
            incoming.locations
        } else retained(current.locations),
        people = if (incoming.people.isMeasuredPosition()) {
            incoming.people
        } else retained(current.people),
        categories = if (incoming.categories.isMeasuredPosition()) {
            incoming.categories
        } else retained(current.categories),
        configuration = incoming.configuration.ifBlank { current.configuration },
        statusBarTop = incoming.statusBarTop ?: current.statusBarTop.takeUnless { configChanged },
        navigationBarBottom = incoming.navigationBarBottom
            ?: current.navigationBarBottom.takeUnless { configChanged }
    )
}

fun initialLibraryInset(
    current: Int,
    cached: Int?,
    sameConfiguration: Boolean,
    visible: Boolean?
): Int = if (current == 0 && sameConfiguration && visible != false) cached ?: 0 else current
