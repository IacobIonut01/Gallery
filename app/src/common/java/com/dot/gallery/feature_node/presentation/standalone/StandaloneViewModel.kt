/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.standalone

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.ChanneledViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandaloneViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ChanneledViewModel() {

    val photoState = mutableStateOf(MediaState())
    val handler = mediaUseCases.mediaHandleUseCase
    var standaloneUri: String? = null
        set(value) {
            if (!value.isNullOrEmpty() && value != standaloneUri) {
                viewModelScope.launch {
                    getMedia(standaloneUri = value)
                }
            }
            field = value
        }

    var clipDataUriList: List<Uri> = emptyList()
        set(value) {
            if (value.isNotEmpty() && value != clipDataUriList) {
                viewModelScope.launch {
                    getMedia(standaloneUri, clipDataUriList)
                }
            }
            field = value
        }

    init {
        getMedia()
    }

    private fun getMedia(standaloneUri: String? = null, clipDataUriList: List<Uri> = emptyList()) {
        if (standaloneUri != null) {
            mediaUseCases.getMediaByUriUseCase(standaloneUri, clipDataUriList.isNotEmpty()).map { result ->
                photoState.value = MediaState(
                    error = if (result is Resource.Error) result.message
                        ?: "An error occurred" else "",
                    media = result.data ?: emptyList()
                )
            }.launchIn(viewModelScope)
            if (clipDataUriList.isNotEmpty()) {
                mediaUseCases.getMediaListByUrisUseCase(clipDataUriList).map { result ->
                    val data = result.data
                    if (data != null) {
                        photoState.value = photoState.value.copy(
                            media = photoState.value.media.toMutableList().apply { addAll(data) }
                        )
                    }
                }.launchIn(viewModelScope)
            }
        }
    }

}