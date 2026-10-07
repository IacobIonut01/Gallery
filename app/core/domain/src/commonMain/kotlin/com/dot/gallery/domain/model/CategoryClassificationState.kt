/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

/**
 * Represents the state of category classification.
 */
data class CategoryClassificationState(
    val isClassifying: Boolean = false,
    val progress: Float = 0f,
    val currentCategory: String? = null,
    val totalCategories: Int = 0,
    val processedCategories: Int = 0,
    val totalMedia: Int = 0,
    val processedMedia: Int = 0
)
