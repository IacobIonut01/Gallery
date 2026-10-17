/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.albums.ui.components

enum class AlbumThumbnailPresentation(
    val allowsMediaRequest: Boolean
) {
    MEDIA(allowsMediaRequest = true),
    LOCKED_PLACEHOLDER(allowsMediaRequest = false)
}

fun albumThumbnailPresentation(isLocked: Boolean): AlbumThumbnailPresentation =
    if (isLocked) {
        AlbumThumbnailPresentation.LOCKED_PLACEHOLDER
    } else {
        AlbumThumbnailPresentation.MEDIA
    }

/**
 * "Change cover" is a sub-page of the album option sheet. It must drop the
 * moment the sheet is dismissed or the system picker returns, otherwise the
 * next long-press reopens on the cover options instead of the album menu.
 */
fun nextAlbumCoverPickerState(
    currentlySelecting: Boolean,
    sheetVisible: Boolean,
    pickerFinished: Boolean = false
): Boolean = currentlySelecting && sheetVisible && !pickerFinished
