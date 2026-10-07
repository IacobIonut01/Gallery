/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.runtime.Immutable


/**
 * Callbacks [CategoriesContent] reports to the app shell. Route strings stay
 * app-side: parameterized destinations get dedicated lambdas.
 */
@Immutable
data class CategoriesActions(
    /** Open a category's media page. */
    val onOpenCategory: (categoryId: Long) -> Unit,
    /** Open the category editor in edit mode (long-press on a tile). */
    val onEditCategory: (categoryId: Long) -> Unit,
    /** Open the category editor in create mode (FAB + empty-state card). */
    val onCreateCategory: () -> Unit,
    /** Open the categories settings page. */
    val onOpenSettings: () -> Unit,
)
