/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.standalone

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.domain.use_case.VaultUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandaloneViewModel @Inject constructor(
    @ApplicationContext
    private val applicationContext: Context,
    private val mediaUseCases: MediaUseCases,
    private val vaultUseCases: VaultUseCases
) : ViewModel() {

    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState = _mediaState.asStateFlow()
    val handler = mediaUseCases.mediaHandleUseCase
    var reviewMode: Boolean = false

    var dataList: List<Uri> = emptyList()
        set(value) {
            if (value.isNotEmpty() && value != dataList) {
                getMedia(value)
            }
            field = value
        }

    var mediaId: Long = -1


    private val _vaults = MutableStateFlow<List<Vault>>(emptyList())
    val vaults = _vaults.asStateFlow()

    fun addMedia(vault: Vault, media: Media) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultUseCases.addMedia(vault, media)
        }
    }

    private fun getMedia(clipDataUriList: List<Uri> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            if (clipDataUriList.isNotEmpty()) {
                mediaUseCases.getMediaListByUrisUseCase(clipDataUriList, reviewMode)
                    .flowOn(Dispatchers.IO)
                    .collectLatest { result ->
                        val data = result.data
                        if (data != null) {
                            mediaId = data.first().id
                            _mediaState.value = MediaState(media = data)
                        } else {
                            _mediaState.value = mediaFromUris()
                        }
                    }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            vaultUseCases.getVaults().collectLatest {
                _vaults.emit(it)
            }
        }
    }

    private fun mediaFromUris(): MediaState {
        val list = mutableListOf<Media>()
        dataList.forEach {
            Media.createFromUri(applicationContext, it)?.let { it1 -> list.add(it1) }
        }
        return MediaState(media = list)
    }

}