/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library

import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.data.dao.CategoryWithMediaCount


/**
 * Data class for category with its thumbnail media
 */
data class CategoryMedia(
    val category: CategoryWithMediaCount,
    val thumbnailMedia: UriMedia?
)

