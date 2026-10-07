/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

/**
 * Represents a category with its associated media count and thumbnail.
 * Used for displaying categories in the UI.
 */
data class CategoryWithCount(
    val category: Category,
    val mediaCount: Int,
    val thumbnailMediaId: Long?
)
