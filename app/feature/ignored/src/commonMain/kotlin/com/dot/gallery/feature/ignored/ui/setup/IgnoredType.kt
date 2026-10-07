/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup

import com.dot.gallery.domain.model.Album

sealed class IgnoredType {

    data class SINGLE(val selectedAlbum: Album?) : IgnoredType()

    data class MULTIPLE(val selectedAlbums: List<Album> = emptyList()) : IgnoredType()

    data class REGEX(val regex: String = "") : IgnoredType()
}
