/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.common

import android.annotation.SuppressLint
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.util.RepeatOnResume
import com.dot.gallery.feature_node.presentation.util.add
import com.dot.gallery.feature_node.presentation.util.collectMedia
import com.dot.gallery.feature_node.presentation.util.mediaFlow
import com.dot.gallery.feature_node.presentation.util.remove
import com.dot.gallery.feature_node.presentation.util.update
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class MediaViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ViewModel() {

    val selectionState = mutableStateOf(false)
    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState = _mediaState.asStateFlow()
    val selectedMediaIdState = mutableStateOf<Set<Long>>(emptySet())
    val handler = mediaUseCases.mediaHandleUseCase

    var albumId: Long = -1L
    var target: String? = null

    var groupByMonth: Boolean = false

    /**
     * Used in PhotosScreen to retrieve all media
     */
    fun launchInPhotosScreen() {
        getMedia(-1, null)
    }

    @SuppressLint("ComposableNaming")
    @Composable
    fun attachToLifecycle() {
        RepeatOnResume {
            getMedia(albumId, target)
        }
    }

    fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            handler.toggleFavorite(result, mediaList, favorite)
        }
    }

    fun toggleSelection(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = mediaState.value.media[index]
            val selectedPhoto = selectedMediaIdState.value.find { it == item.id }
            if (selectedPhoto != null) {
                selectedMediaIdState.remove(selectedPhoto)
            } else {
                selectedMediaIdState.add(item.id)
            }
            selectionState.update(selectedMediaIdState.value.isNotEmpty())
        }
    }

    private fun getMedia(albumId: Long = -1L, target: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaUseCases.mediaFlow(albumId, target).collectLatest { result ->
                val data = result.data ?: emptyList()
                val error = if (result is Resource.Error) result.message
                    ?: "An error occurred" else ""
                if (data.isEmpty()) {
                    return@collectLatest _mediaState.emit(MediaState(isLoading = false))
                }
                if (data == _mediaState.value.media) return@collectLatest
                return@collectLatest _mediaState.collectMedia(
                    data = data,
                    error = error,
                    albumId = albumId,
                    groupByMonth = groupByMonth,
                )
            }
        }
    }

}