/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albums

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.AlbumState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ViewModel() {

    val albumsState = mutableStateOf(AlbumState())
    val pinnedAlbumState = mutableStateOf(AlbumState())

    init {
        getAlbums()
    }

    fun updateOrder(mediaOrder: MediaOrder) {
        viewModelScope.launch {
            albumsState.value = albumsState.value.copy(
                albums = mediaOrder.sortAlbums(albumsState.value.albums)
            )
        }
    }

    fun toggleAlbumPin(album: Album, isPinned: Boolean = true) {
        viewModelScope.launch {
            val newAlbum = album.copy(isPinned = isPinned)
            if (isPinned) {
                // Insert pinnedAlbumId to Database
                mediaUseCases.insertPinnedAlbumUseCase(newAlbum)
                // Remove original Album from unpinned List
                albumsState.value = albumsState.value.copy(
                    albums = albumsState.value.albums.minus(album)
                )
                // Add 'pinned' version of the album object to the pinned List
                pinnedAlbumState.value = pinnedAlbumState.value.copy(
                    albums = pinnedAlbumState.value.albums.toMutableList().apply { add(newAlbum) }
                )
            } else {
                // Delete pinnedAlbumId from Database
                mediaUseCases.deletePinnedAlbumUseCase(album)
                // Add 'un-pinned' version of the album object to the pinned List
                albumsState.value = albumsState.value.copy(
                    albums = albumsState.value.albums.toMutableList().apply { add(newAlbum) }
                )
                // Remove original Album from pinned List
                pinnedAlbumState.value = pinnedAlbumState.value.copy(
                    albums = pinnedAlbumState.value.albums.minus(album)
                )
            }
        }
    }

    private fun getAlbums(mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)) {
        mediaUseCases.getAlbumsUseCase(mediaOrder).map { result ->
            // Result data list
            val data = result.data ?: emptyList()
            // Create a copy of the result Data and remove unpinned Albums
            val pinnedData = ArrayList(data).apply { removeAll { !it.isPinned } }
            // Create a copy of the result Data and remove pinned Albums
            val albumData = ArrayList(data).apply { removeAll(pinnedData.toSet()) }
            val error = if (result is Resource.Error) result.message ?: "An error occurred" else ""
            val isLoading = result is Resource.Loading
            albumsState.value = AlbumState(
                error = error,
                isLoading = isLoading,
                albums = albumData
            )
            pinnedAlbumState.value = AlbumState(
                error = error,
                isLoading = isLoading,
                albums = pinnedData
            )
        }.launchIn(viewModelScope)
    }

}