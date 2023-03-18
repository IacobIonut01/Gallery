package com.dot.gallery.core

import android.os.Parcelable
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaState(
    val isLoading: Boolean = false,
    val media: List<Media> = emptyList(),
    val error: String = ""
) : Parcelable

@Parcelize
data class AlbumState(
    val isLoading: Boolean = false,
    val albums: List<Album> = emptyList(),
    val error: String = ""
) : Parcelable