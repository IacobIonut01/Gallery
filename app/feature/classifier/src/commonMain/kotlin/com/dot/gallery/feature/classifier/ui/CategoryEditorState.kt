/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.domain.model.Category
import com.dot.gallery.domain.model.UriMedia


/** Snapshot of the unified category editor — collected app-side. */
@Stable
data class CategoryEditorState(
    /** null/`<=0` = create mode. */
    val categoryId: Long? = null,
    val category: Category? = null,
    val categoryName: String = "",
    val searchTerms: String = "",
    val threshold: Float = Category.DEFAULT_THRESHOLD,
    val referenceImageIds: List<Long> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val previewMedia: List<UriMedia> = emptyList(),
    val previewCount: Int = 0,
    /** Timeline media — resolves reference-image thumbnails by id. */
    val allMedia: List<UriMedia> = emptyList(),
) {
    val isEditMode: Boolean get() = categoryId != null && categoryId > 0
    val isValid: Boolean
        get() = categoryName.isNotBlank() &&
                (searchTerms.isNotBlank() || referenceImageIds.isNotEmpty())
}
