/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.standalone

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StandaloneViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ViewModel() {

    val photoState = mutableStateOf(MediaState())
    val handler = mediaUseCases.mediaHandleUseCase
    var standaloneUri: String? = null
        set(value) {
            if (!value.isNullOrEmpty() && value != standaloneUri) {
                getMedia(standaloneUri = value)
            }
            field = value
        }

    var clipDataUriList: List<Uri> = emptyList()
        set(value) {
            if (value.isNotEmpty() && value != clipDataUriList) {
                getMedia(standaloneUri, clipDataUriList)
            }
            field = value
        }

    init {
        getMedia()
    }

    private fun getMedia(standaloneUri: String? = null, clipDataUriList: List<Uri> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            if (standaloneUri != null) {
                mediaUseCases.getMediaByUriUseCase(standaloneUri, clipDataUriList.isNotEmpty()).onEach { result ->
                    withContext(Dispatchers.Main) {
                        photoState.value = MediaState(
                            error = if (result is Resource.Error) result.message
                                ?: "An error occurred" else "",
                            media = result.data ?: emptyList()
                        )
                    }
                }.flowOn(Dispatchers.IO).collect()
                if (clipDataUriList.isNotEmpty()) {
                    mediaUseCases.getMediaListByUrisUseCase(clipDataUriList).onEach { result ->
                        val data = result.data
                        if (data != null) {
                            withContext(Dispatchers.Main) {
                                photoState.value = photoState.value.copy(
                                    media = photoState.value.media.toMutableList()
                                        .apply { addAll(data) }
                                )
                            }
                        }
                    }.flowOn(Dispatchers.IO).collect()
                }
            }
        }
    }

}