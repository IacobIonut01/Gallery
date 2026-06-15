/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloudProfileUiState(
    val serverName: String = "",
    val serverUrl: String = "",
    val userId: String? = null,
    val providerType: ProviderType? = null,
    val storageInfo: CloudStorageInfo? = null,
    val cachedMediaCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CloudProfileViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudProfileUiState())
    val uiState: StateFlow<CloudProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }
        val provider = providers.first()
        _uiState.value = _uiState.value.copy(
            serverName = provider.displayName,
            providerType = provider.providerType
        )
        viewModelScope.launch {
            try {
                val count = cloudMediaDao.countCached()
                _uiState.value = _uiState.value.copy(cachedMediaCount = count)
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                val result = provider.getStorageInfo()
                result.onSuccess { info ->
                    _uiState.value = _uiState.value.copy(
                        storageInfo = info,
                        isLoading = false
                    )
                }.onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun disconnect() {
        val type = _uiState.value.providerType ?: return
        viewModelScope.launch {
            repository.disconnect(type)
            repository.clearCache(type)
            _uiState.value = CloudProfileUiState()
        }
    }
}
