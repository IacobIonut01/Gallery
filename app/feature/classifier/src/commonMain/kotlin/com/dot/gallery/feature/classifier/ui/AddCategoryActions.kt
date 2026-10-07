/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.runtime.Immutable


/** Callbacks [AddCategoryContent] reports to the app shell. */
@Immutable
data class AddCategoryActions(
    val onNameChange: (name: String) -> Unit,
    val onSearchTermsChange: (terms: String) -> Unit,
    val onThresholdChange: (threshold: Float) -> Unit,
    /** Persist the new category; [onDone] runs after the save completes. */
    val onSave: (onDone: () -> Unit) -> Unit,
    /** Toolbar back / post-save dismissal. */
    val onNavigateBack: () -> Unit,
    /** Open a preview match in the viewer (`idAndQuery` route — app-side). */
    val onOpenPreviewMedia: (mediaId: Long) -> Unit,
)
