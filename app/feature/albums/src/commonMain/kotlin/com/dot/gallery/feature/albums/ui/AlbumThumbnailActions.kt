/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.albums.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.domain.model.Album
import kotlinx.coroutines.flow.Flow

/**
 * Seam for the platform "custom album cover" actions that need the media
 * handler and a system image picker. The app wires this to `MediaHandler` +
 * an activity-result picker; other platforms (or previews) pass `null` to hide
 * the option entirely.
 */
@Stable
class AlbumThumbnailActions(
    /** Whether the album currently has a user-set custom cover. */
    val hasThumbnail: (albumId: Long) -> Flow<Boolean>,
    /** Launch the image picker flow for [album]'s new cover. */
    val onPickThumbnail: (album: Album) -> Unit,
    /** Remove the user-set cover for [albumId]. */
    val onDeleteThumbnail: (albumId: Long) -> Unit,
)
