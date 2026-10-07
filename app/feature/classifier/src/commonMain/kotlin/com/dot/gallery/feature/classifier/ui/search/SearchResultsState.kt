/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui.search

import androidx.compose.runtime.Stable
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.UriMedia


/**
 * Snapshot of the search screen's result pipeline. Produced app-side by the
 * search ViewModel and consumed by [SearchContent].
 */
@Stable
data class SearchResultsState(
    val hasSearched: Boolean = false,
    val isSearching: Boolean = false,
    val isRelevanceSearch: Boolean = false,
    val progress: Float = 0f,
    val results: MediaState<UriMedia> = MediaState(isLoading = false),
    val dateResults: MediaState<UriMedia>? = null
)
