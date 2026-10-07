/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui

import androidx.compose.runtime.Immutable


/**
 * Callbacks [LibraryContent] reports to the app shell. Route strings stay
 * app-side: fixed destinations come through [LibraryRoutes] (used with
 * [onNavigate]), parameterized ones get their own callback.
 */
@Immutable
data class LibraryActions(
    /** Push a plain navigation route (shortcut tiles, section headers, settings). */
    val onNavigate: (route: String) -> Unit,
    /** Open the location timeline for a place (called only when the row is resolvable). */
    val onOpenLocation: (city: String, country: String, latitude: Double?, longitude: Double?) -> Unit,
    /** Open the media viewer for [mediaId] with no album scope. */
    val onOpenMedia: (mediaId: Long) -> Unit,
    /** Open a person detail page. */
    val onOpenPerson: (configId: Long, personId: String) -> Unit,
    /** Open a category page. */
    val onOpenCategory: (categoryId: Long) -> Unit,
    /** Open the category editor (long-press on a category tile). */
    val onEditCategory: (categoryId: Long) -> Unit,
    /** Persisted-viewport updates (scroll positions merged upstream). */
    val onViewportChanged: (LibraryViewport) -> Unit,
    /** Pinch-zoom changed the album-grid cell index — persist it app-side. */
    val onCellsIndexChange: (Int) -> Unit,
    /** Startup/frame tracing hook — StartupTracer app-side, no-op by default. */
    val trace: (String) -> Unit = {},
)
