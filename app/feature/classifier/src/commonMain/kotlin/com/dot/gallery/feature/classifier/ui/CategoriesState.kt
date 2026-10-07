/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.data.dao.CategoryWithMediaCount
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.UriMedia


/** Snapshot of the categories grid — collected app-side, forwarded as one value. */
@Stable
data class CategoriesState(
    val categoriesWithCount: List<CategoryWithMediaCount> = emptyList(),
    val distinctMediaCount: Int = 0,
    /** Timeline media — used to resolve category thumbnails by id. */
    val mediaState: MediaState<UriMedia> = MediaState(),
)
