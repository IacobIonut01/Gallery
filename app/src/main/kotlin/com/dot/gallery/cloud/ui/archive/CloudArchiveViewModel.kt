/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudArchiveViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry
) : ViewModel() {

    private val _mediaState = MutableStateFlow(MediaState<Media.UriMedia>())
    val mediaState: StateFlow<MediaState<Media.UriMedia>> = _mediaState.asStateFlow()

    init {
        loadArchived()
    }

    fun loadArchived() {
        _mediaState.value = _mediaState.value.copy(isLoading = true, error = "")
        viewModelScope.launch {
            try {
                val cached = repository.getCachedArchivedAsync()
                if (cached.isNotEmpty()) {
                    val media = cached.map { it.toUriMedia() }
                    _mediaState.value = MediaState(
                        media = media,
                        isLoading = false
                    )
                    return@launch
                }
                val providers = registry.getRemoteProviders().filter { it.isAvailable }
                if (providers.isEmpty()) {
                    _mediaState.value = MediaState(isLoading = false)
                    return@launch
                }
                repository.getRemoteArchived(providers.first().providerType).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val media = resource.data?.map { it.toUriMedia() } ?: emptyList()
                            _mediaState.value = MediaState(
                                media = media,
                                isLoading = false
                            )
                        }
                        is Resource.Error -> {
                            _mediaState.value = MediaState(
                                isLoading = false,
                                error = resource.message ?: ""
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _mediaState.value = MediaState(
                    isLoading = false,
                    error = e.message ?: ""
                )
            }
        }
    }

    fun unarchive(remoteId: String) {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        viewModelScope.launch {
            repository.toggleArchive(providers.first().providerType, remoteId, false)
                .onSuccess {
                    _mediaState.value = _mediaState.value.copy(
                        media = _mediaState.value.media.filter {
                            val cloudUri = it.uri.toString()
                            !cloudUri.contains(remoteId)
                        }
                    )
                }
        }
    }
}
