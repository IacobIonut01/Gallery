/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.presentation.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.util.collectMedia
import com.dot.gallery.feature_node.presentation.util.mediaFlowWithType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class PickerViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
): ViewModel() {

    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState = _mediaState.asStateFlow()

    fun init() = getMedia(albumId, allowedMedia)

    var allowedMedia: AllowedMedia = AllowedMedia.BOTH

    var albumId: Long = -1L

    private fun getMedia(albumId: Long, allowedMedia: AllowedMedia) {
        viewModelScope.launch {
            mediaUseCases.mediaFlowWithType(albumId, allowedMedia).flowOn(Dispatchers.IO).collectLatest { result ->
                val data = result.data ?: emptyList()
                val error = if (result is Resource.Error) result.message
                    ?: "An error occurred" else ""
                if (data.isEmpty()) {
                    return@collectLatest _mediaState.emit(MediaState())
                }
                _mediaState.collectMedia(data, error, albumId)
            }
        }

    }
}

enum class AllowedMedia {
    PHOTOS, VIDEOS, BOTH
}