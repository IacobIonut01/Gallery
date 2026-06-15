/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncStatusUiState(
    val totalRemote: Int = 0,
    val totalCached: Int = 0,
    val synced: Int = 0,
    val remoteOnly: Int = 0,
    val localOnly: Int = 0,
    val pendingUpload: Int = 0,
    val isLoading: Boolean = false,
    val lastSyncError: String? = null
)

@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncStatusUiState())
    val uiState: StateFlow<SyncStatusUiState> = _uiState.asStateFlow()

    val syncedMedia = cloudMediaDao.getBySyncState(SyncState.SYNCED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remoteOnlyMedia = cloudMediaDao.getBySyncState(SyncState.REMOTE_ONLY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingUploadMedia = cloudMediaDao.getBySyncState(SyncState.UPLOAD_PENDING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSyncStatus()
    }

    fun loadSyncStatus() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val cached = cloudMediaDao.countCached()
                _uiState.value = _uiState.value.copy(
                    totalCached = cached,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastSyncError = e.message
                )
            }
        }
    }

    fun triggerSync() {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        _uiState.value = _uiState.value.copy(isLoading = true, lastSyncError = null)
        viewModelScope.launch {
            try {
                repository.getAllRemoteAssets(0, 200).collect { resource ->
                    when (resource) {
                        is com.dot.gallery.core.Resource.Success -> {
                            loadSyncStatus()
                        }
                        is com.dot.gallery.core.Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                lastSyncError = resource.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastSyncError = e.message
                )
            }
        }
    }
}
