/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup

import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.IgnoredAlbum

data class IgnoredOptionsUiState(
    val currentStep: OptionsStep = OptionsStep.OPTIONS,
    val ignoredAlbum: IgnoredAlbum? = null,
    val editLocation: Int = IgnoredAlbum.ALBUMS_ONLY,
    val editRegex: String = "",
    val editSelectedAlbums: List<Album> = emptyList(),
    val editMatchedAlbums: List<Album> = emptyList(),
    val albums: List<Album> = emptyList(),
    /** Index into `Constants.albumCellsList` — fed app-side from the album grid-size preference. */
    val albumGridSize: Int = 3,
    val canProceed: Boolean = false
) {
    val isWildcard: Boolean get() = ignoredAlbum?.wildcard != null
    val isMultiple: Boolean get() = (ignoredAlbum?.albumIds?.size ?: 0) > 1
    val isSingle: Boolean get() = !isWildcard && !isMultiple
}
