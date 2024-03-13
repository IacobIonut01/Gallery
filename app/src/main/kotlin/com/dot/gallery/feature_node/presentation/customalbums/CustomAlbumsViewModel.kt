/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albums

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.R
import com.dot.gallery.core.CustomAlbumState
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.FilterOption
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.CustomAlbum
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.presentation.util.RepeatOnResume
import com.dot.gallery.feature_node.presentation.util.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomAlbumsViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ViewModel() {

    private val _albumsState = MutableStateFlow(CustomAlbumState())
    val albumsState = _albumsState.asStateFlow()
    private val _unPinnedAlbumsState = MutableStateFlow(CustomAlbumState())
    val unPinnedAlbumsState = _unPinnedAlbumsState.asStateFlow()
    private val _pinnedAlbumState = MutableStateFlow(CustomAlbumState())
    val pinnedAlbumState = _pinnedAlbumState.asStateFlow()
    val handler = mediaUseCases.mediaHandleUseCase

    fun onAlbumClick(navigate: (String) -> Unit): (CustomAlbum) -> Unit = { album ->
        navigate(Screen.AlbumViewScreen.route + "?albumId=${album.id}&albumName=${album.label}")
    }

    val onAlbumLongClick: (CustomAlbum) -> Unit = { album ->
        toggleAlbumPin(album, !album.isPinned)
    }

    @SuppressLint("ComposableNaming")
    @Composable
    fun attachToLifecycle() {
        RepeatOnResume {
            getCustomAlbums()
        }
    }

    @Composable
    fun rememberFilters(): SnapshotStateList<FilterOption> {
        val lastValue by Settings.Album.rememberLastSort()
        return remember(lastValue) {
            mutableStateListOf(
                FilterOption(
                    titleRes = R.string.filter_recent,
                    mediaOrder = MediaOrder.Date(OrderType.Descending),
                    onClick = { updateOrder(it) },
                    selected = lastValue == 0
                ),
                FilterOption(
                    titleRes = R.string.filter_old,
                    mediaOrder = MediaOrder.Date(OrderType.Ascending),
                    onClick = { updateOrder(it) },
                    selected = lastValue == 1
                ),

                FilterOption(
                    titleRes = R.string.filter_nameAZ,
                    mediaOrder = MediaOrder.Label(OrderType.Ascending),
                    onClick = { updateOrder(it) },
                    selected = lastValue == 2
                ),
                FilterOption(
                    titleRes = R.string.filter_nameZA,
                    mediaOrder = MediaOrder.Label(OrderType.Descending),
                    onClick = { updateOrder(it) },
                    selected = lastValue == 3
                )
            )
        }
    }

    private fun updateOrder(mediaOrder: MediaOrder) {
        getCustomAlbums(mediaOrder)
    }

    private fun toggleAlbumPin(album: CustomAlbum, isPinned: Boolean = true) {
        // todo
    }

    fun createNewAlbum(name: String) {
        viewModelScope.launch( Dispatchers.IO ) {
            mediaUseCases.customAlbumsUseCase.add(CustomAlbum(label = name, timestamp = System.currentTimeMillis()))
        }
    }

    suspend fun createAndReturnNewAlbum(name: String): CustomAlbum {
        return  mediaUseCases.customAlbumsUseCase.add(CustomAlbum(label = name, timestamp = System.currentTimeMillis()))
    }

    val onAlbumDelete: (CustomAlbum) -> Unit = { album ->
        viewModelScope.launch( Dispatchers.IO ) {
            mediaUseCases.customAlbumsUseCase.delete(album)
        }
    }



    fun addMediaToAlbum(customAlbum: CustomAlbum, mediaid: Long){
        viewModelScope.launch( Dispatchers.IO ) {
            mediaUseCases.customAlbumsUseCase.addMediaToAlbum(customAlbum, mediaid)
        }
    }


    private fun getCustomAlbums(mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)) {
        viewModelScope.launch(Dispatchers.IO) {


            mediaUseCases.customAlbumsUseCase(mediaOrder).collect{ result ->
                // Result data list

                val newAlbumState = CustomAlbumState(result)
                if (result == albumsState.value.albums) return@collect
                val newUnPinnedAlbumState = CustomAlbumState(result.filter { !it.isPinned })
                val newPinnedState = CustomAlbumState(result.filter { it.isPinned }.sortedBy { it.label })
                if (unPinnedAlbumsState.value != newUnPinnedAlbumState) {
                    _unPinnedAlbumsState.emit(newUnPinnedAlbumState)
                }
                if (pinnedAlbumState.value != newPinnedState) {
                    _pinnedAlbumState.emit(newPinnedState)
                }
                if (albumsState.value != newAlbumState) {
                    _albumsState.emit(newAlbumState)
                }
            }
        }
    }

}