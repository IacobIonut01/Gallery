/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.presentation.picker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.feature_node.domain.model.AlbumState
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.util.mapMedia
import com.dot.gallery.feature_node.presentation.util.mediaFlowWithType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
open class PickerViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {
    var allowedMedia: AllowedMedia = AllowedMedia.BOTH
    var albumId: Long = -1L
        set(value) {
            field = value
            mediaState = lazy {
                repository.mediaFlowWithType(value, allowedMedia)
                    .mapMedia(albumId = value, groupByMonth = false, withMonthHeader = false, updateDatabase = {})
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), MediaState())
            }
        }

    var mediaState = lazy {
        repository.mediaFlowWithType(albumId, allowedMedia)
            .mapMedia(albumId = albumId, groupByMonth = false, withMonthHeader = false, updateDatabase = {})
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), MediaState())
    }

    val albumsState by lazy {
        repository.getAlbumsWithType(allowedMedia)
            .map { result ->
                val data = result.data ?: emptyList()
                val error = if (result is Resource.Error) result.message
                    ?: "An error occurred" else ""
                if (data.isEmpty()) {
                    return@map AlbumState(albums = listOf(emptyAlbum), error = error)
                }
                val albums = mutableListOf<Album>().apply {
                    add(emptyAlbum)
                    addAll(data)
                }
                AlbumState(albums = albums, error = error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AlbumState())
    }


    private val emptyAlbum = Album(id = -1, label = "All", uri = Uri.EMPTY, pathToThumbnail = "", timestamp = 0, relativePath = "")
}

enum class AllowedMedia {
    PHOTOS, VIDEOS, BOTH;

    override fun toString(): String {
        return when (this) {
            PHOTOS -> "image%"
            VIDEOS -> "video%"
            BOTH -> "%/%"
        }
    }

    fun toStringAny(): String {
        return when (this) {
            PHOTOS -> "image/*"
            VIDEOS -> "video/*"
            BOTH -> "*/*"
        }
    }
}