/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.runtime.Immutable


/** Callbacks [EditCategoryContent] reports to the app shell. */
@Immutable
data class EditCategoryActions(
    /** Load the category for editing (called once per [EditCategoryState.categoryId]). */
    val onLoadCategory: (categoryId: Long) -> Unit,
    val onNameChange: (name: String) -> Unit,
    val onSearchTermsChange: (terms: String) -> Unit,
    val onThresholdChange: (threshold: Float) -> Unit,
    /** Persist edits; [onDone] runs after the save completes. */
    val onSave: (onDone: () -> Unit) -> Unit,
    /** Delete the category; [onDone] runs after the delete completes. */
    val onDelete: (onDone: () -> Unit) -> Unit,
    /** Toolbar back / post-save dismissal. */
    val onNavigateBack: () -> Unit,
    /** Open a preview match in the viewer (`idAndCategoryId` route — app-side). */
    val onOpenPreviewMedia: (mediaId: Long) -> Unit,
)
