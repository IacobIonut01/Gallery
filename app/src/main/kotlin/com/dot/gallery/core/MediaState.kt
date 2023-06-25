/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class MediaState(
    val media: List<Media> = emptyList(),
    val mappedMedia: List<MediaItem> = emptyList(),
    val mappedMediaWithMonthly: List<MediaItem> = emptyList(),
    val dateHeader: String = "",
    val error: String = "",
    val isLoading: Boolean = false
) : Parcelable


@Immutable
@Parcelize
data class AlbumState(
    val albums: List<Album> = emptyList(),
    val error: String = ""
) : Parcelable