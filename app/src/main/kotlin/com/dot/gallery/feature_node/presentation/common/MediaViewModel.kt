/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.common

import android.annotation.SuppressLint
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.util.RepeatOnResume
import com.dot.gallery.feature_node.presentation.util.collectMedia
import com.dot.gallery.feature_node.presentation.util.mediaFlow
import com.dot.gallery.feature_node.presentation.util.update
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class MediaViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ViewModel() {

    val multiSelectState = mutableStateOf(false)
    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState = _mediaState.asStateFlow()
    val selectedPhotoState = mutableStateListOf<Media>()
    val handler = mediaUseCases.mediaHandleUseCase

    var albumId: Long = -1L
        set(value) {
            getMedia(albumId = value)
            field = value
        }
    var target: String? = null
        set(value) {
            getMedia(target = value)
            field = value
        }

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
        viewModelScope.launch {
            val item = mediaState.value.media[index]
            val selectedPhoto = selectedPhotoState.find { it.id == item.id }
            if (selectedPhoto != null) {
                selectedPhotoState.remove(selectedPhoto)
            } else {
                selectedPhotoState.add(item)
            }
            multiSelectState.update(selectedPhotoState.isNotEmpty())
        }
    }

    private fun getMedia(albumId: Long = -1L, target: String? = null) {
        viewModelScope.launch {
            mediaUseCases.mediaFlow(albumId, target).flowOn(Dispatchers.IO).collectLatest { result ->
                val data = result.data ?: emptyList()
                if (data == mediaState.value.media) return@collectLatest
                val error = if (result is Resource.Error) result.message
                    ?: "An error occurred" else ""
                if (data.isEmpty()) {
                    return@collectLatest _mediaState.emit(MediaState())
                }
                return@collectLatest _mediaState.collectMedia(
                    data = data,
                    error = error,
                    albumId = albumId,
                    groupByMonth = groupByMonth
                )
            }
        }
    }

}