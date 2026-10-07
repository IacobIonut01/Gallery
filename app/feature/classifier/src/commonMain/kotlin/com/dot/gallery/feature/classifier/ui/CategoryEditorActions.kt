/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.runtime.Immutable
import com.dot.gallery.domain.model.Category


/**
 * Callbacks [CategoryEditorContent] reports to the app shell — these map to the
 * editor ViewModel plus navigation back.
 */
@Immutable
data class CategoryEditorActions(
    /** Load an existing category for editing (called once per [CategoryEditorState.categoryId]). */
    val onLoadCategory: (categoryId: Long) -> Unit,
    val onNameChange: (name: String) -> Unit,
    val onSearchTermsChange: (terms: String) -> Unit,
    val onThresholdChange: (threshold: Float) -> Unit,
    /** Quick-start template chip. */
    val onApplyTemplate: (template: Category) -> Unit,
    /** Reference image picked in the media picker sheet. */
    val onAddReferenceImage: (mediaId: Long) -> Unit,
    val onRemoveReferenceImage: (mediaId: Long) -> Unit,
    /** Persist the category; [onDone] runs after the save completes. */
    val onSave: (onDone: () -> Unit) -> Unit,
    /** Delete the category; [onDone] runs after the delete completes. */
    val onDelete: (onDone: () -> Unit) -> Unit,
    /** Toolbar back / post-save dismissal. */
    val onNavigateBack: () -> Unit,
)
