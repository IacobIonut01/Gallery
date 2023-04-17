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

    init {
        getAlbums()
    }

    fun updateOrder(mediaOrder: MediaOrder) {
        viewModelScope.launch {
            getAlbums(mediaOrder)
        }
    }

    private fun getAlbums(mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)) {
        mediaUseCases.getAlbumsUseCase(mediaOrder).map { result ->
            albumsState.value = AlbumState(
                error = if (result is Resource.Error) result.message ?: "An error occurred" else "",
                isLoading = result is Resource.Loading,
                albums = result.data ?: emptyList()
            )
        }.launchIn(viewModelScope)
    }

}