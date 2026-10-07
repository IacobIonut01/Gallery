/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup

import com.dot.gallery.domain.model.Album

sealed interface IgnoredOptionsAction {
    data object NavigateBack : IgnoredOptionsAction
    data object NavigateNext : IgnoredOptionsAction
    data object Cancel : IgnoredOptionsAction
    data object Confirm : IgnoredOptionsAction
    data object Delete : IgnoredOptionsAction
    data object Edit : IgnoredOptionsAction
    data class SetLocation(val location: Int) : IgnoredOptionsAction
    data class SetRegex(val regex: String) : IgnoredOptionsAction
    data class ToggleAlbum(val album: Album) : IgnoredOptionsAction
    data class SelectAlbum(val album: Album) : IgnoredOptionsAction
}
