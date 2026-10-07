/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.domain.model.Category


/** Snapshot of the add-category form — collected app-side. */
@Stable
data class AddCategoryState(
    val categoryName: String = "",
    val searchTerms: String = "",
    val threshold: Float = Category.DEFAULT_THRESHOLD,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val previewCount: Int = 0,
) {
    val isValid: Boolean get() = categoryName.isNotBlank() && searchTerms.isNotBlank()
}
