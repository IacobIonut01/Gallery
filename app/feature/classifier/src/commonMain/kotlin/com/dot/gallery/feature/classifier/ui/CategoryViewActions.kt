/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.runtime.Immutable


/** Callbacks [CategoryViewContent] reports to the app shell. */
@Immutable
data class CategoryViewActions(
    /** The route arg is unusable (empty legacy name / non-positive id) → navigate up. */
    val onInvalidCategory: () -> Unit,
    /** Load media + details for the id route (maps to `setCategoryId`). Unused on the legacy route. */
    val onLoadCategory: (categoryId: Long) -> Unit,
    /** Toolbar edit affordance on the id route → CategoryEditorScreen.edit (app-side). */
    val onEditCategory: (categoryId: Long) -> Unit,
    /** Open media in the viewer (`idAndCategory`/`idAndCategoryId` routes — app-side). */
    val onOpenMedia: (mediaId: Long) -> Unit,
    /** Search-bar click → SearchScreen route (app-side). */
    val onSearchClick: () -> Unit,
)
