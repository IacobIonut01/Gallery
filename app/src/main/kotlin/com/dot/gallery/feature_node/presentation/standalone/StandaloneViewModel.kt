/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.standalone

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState = _mediaState.asStateFlow()
    val handler = mediaUseCases.mediaHandleUseCase

    var dataList: List<Uri> = emptyList()
        set(value) {
            if (value.isNotEmpty() && value != dataList) {
                getMedia(value)
            }
            field = value
        }

    var mediaId: Long = -1

    private fun getMedia(clipDataUriList: List<Uri> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            if (clipDataUriList.isNotEmpty()) {
                mediaUseCases.getMediaListByUrisUseCase(clipDataUriList).onEach { result ->
                    val data = result.data
                    if (data != null) {
                        withContext(Dispatchers.Main) {
                            _mediaState.emit(MediaState(media = data))
                            mediaId = data.first().id
                        }
                    }
                }.flowOn(Dispatchers.IO).collect()
            }
        }
    }

}