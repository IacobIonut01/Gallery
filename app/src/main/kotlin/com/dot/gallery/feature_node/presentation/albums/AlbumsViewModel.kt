/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albums

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.R
import com.dot.gallery.core.Resource
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.FilterKind
import com.dot.gallery.core.presentation.components.FilterOption
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.AlbumState
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.domain.model.PinnedAlbum
import com.dot.gallery.feature_node.domain.model.TimelineSettings
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.presentation.util.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MediaRepository,
    val handler: MediaHandleUseCase
) : ViewModel() {

    fun onAlbumClick(navigate: (String) -> Unit): (Album) -> Unit = { album ->
        navigate(Screen.AlbumViewScreen.route + "?albumId=${album.id}&albumName=${album.label}")
    }

    val onAlbumLongClick: (Album) -> Unit = { album ->
        toggleAlbumPin(album, !album.isPinned)
    }

    fun moveAlbumToTrash(result: ActivityResultLauncher<IntentSenderRequest>, album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repository.getMediaByAlbumId(album.id).firstOrNull()
            val data = response?.data ?: emptyList()
            repository.trashMedia(result, data, true)
        }
    }

    @Composable
    fun rememberFilters(): SnapshotStateList<FilterOption> {
        val lastValue by Settings.Album.rememberLastSort()
        return remember(lastValue) {
            mutableStateListOf(
                FilterOption(
                    titleRes = R.string.filter_type_date,
                    filterKind = FilterKind.DATE,
                    onClick = { albumOrder = it }
                ),
                FilterOption(
                    titleRes = R.string.filter_type_name,
                    filterKind = FilterKind.NAME,
                    onClick = { albumOrder = it }
                )
            )
        }
    }

    private fun toggleAlbumPin(album: Album, isPinned: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isPinned) {
                repository.insertPinnedAlbum(PinnedAlbum(album.id))
            } else {
                repository.removePinnedAlbum(PinnedAlbum(album.id))
            }
        }
    }

    private val settingsFlow = repository.getTimelineSettings()
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, TimelineSettings())

    private val pinnedAlbums = repository.getPinnedAlbums()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val blacklistedAlbums = repository.getBlacklistedAlbums()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    private var albumOrder: MediaOrder
        get() = settingsFlow.value?.albumMediaOrder ?: MediaOrder.Date(OrderType.Descending)
        set(value) {
            viewModelScope.launch(Dispatchers.IO) {
                settingsFlow.value?.copy(albumMediaOrder = value)?.let {
                    repository.updateTimelineSettings(it)
                }
            }
        }

    val albumsFlow = combine(
        repository.getAlbums(mediaOrder = albumOrder),
        pinnedAlbums,
        blacklistedAlbums,
        settingsFlow
    ) { result, pinnedAlbums, blacklistedAlbums, settings ->
        val newOrder = settings?.albumMediaOrder ?: albumOrder
        val data = newOrder.sortAlbums(result.data ?: emptyList())
        val cleanData = data.removeBlacklisted(blacklistedAlbums).mapPinned(pinnedAlbums)

        AlbumState(
            albums = cleanData,
            albumsWithBlacklisted = data,
            albumsUnpinned = cleanData.filter { !it.isPinned },
            albumsPinned = cleanData.filter { it.isPinned }.sortedBy { it.label },
            isLoading = false,
            error = if (result is Resource.Error) result.message ?: "An error occurred" else ""
        )
    }.stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(), AlbumState())

    private fun List<Album>.mapPinned(pinnedAlbums: List<PinnedAlbum>): List<Album> =
        map { album -> album.copy(isPinned = pinnedAlbums.any { it.id == album.id }) }

    private fun List<Album>.removeBlacklisted(blacklistedAlbums: List<IgnoredAlbum>): List<Album> =
        toMutableList().apply {
            removeAll { album -> blacklistedAlbums.any { it.matchesAlbum(album) && it.hiddenInAlbums } }
        }

}