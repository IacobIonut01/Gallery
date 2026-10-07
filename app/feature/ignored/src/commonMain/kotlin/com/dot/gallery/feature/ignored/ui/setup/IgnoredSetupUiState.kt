/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup

import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.IgnoredAlbum

data class IgnoredSetupUiState(
    val currentStep: SetupStep = SetupStep.TYPE_SELECTION,
    val location: Int = IgnoredAlbum.ALBUMS_ONLY,
    val type: IgnoredType = IgnoredType.SINGLE(null),
    val matchedAlbums: List<Album> = emptyList(),
    val albums: List<Album> = emptyList(),
    val ignoredAlbums: List<IgnoredAlbum> = emptyList(),
    /** Index into `Constants.albumCellsList` — fed app-side from the album grid-size preference. */
    val albumGridSize: Int = 3,
    val canProceed: Boolean = false
) {
    val isWildcard: Boolean get() = type is IgnoredType.REGEX
    val isMultiple: Boolean get() = type is IgnoredType.MULTIPLE
    val isSingle: Boolean get() = type is IgnoredType.SINGLE

    val selectedAlbums: List<Album> get() = when (type) {
        is IgnoredType.SINGLE -> listOfNotNull(type.selectedAlbum)
        is IgnoredType.MULTIPLE -> type.selectedAlbums
        is IgnoredType.REGEX -> emptyList()
    }

    val regex: String get() = (type as? IgnoredType.REGEX)?.regex ?: ""
}
