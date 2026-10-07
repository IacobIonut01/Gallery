/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.dot.gallery.core.Resource
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.sync.CloudDownloadWorker


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

class SyncStatusViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao,
    private val scheduler: BackgroundTaskScheduler
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

    /** Enqueues a one-time pass that downloads every REMOTE_ONLY item across enabled accounts. */
    fun downloadAll() {
        viewModelScope.launch { CloudDownloadWorker.triggerNow(scheduler) }
    }

    fun triggerSync() {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        _uiState.value = _uiState.value.copy(isLoading = true, lastSyncError = null)
        viewModelScope.launch {
            try {
                // Page through ALL remote assets instead of only the first 200 — a single fixed
                // page silently truncated larger libraries. Stop when a page comes back empty.
                var page = 0
                while (page < MAX_SYNC_PAGES) {
                    val resource = repository.getAllRemoteAssets(page, SYNC_PAGE_SIZE).first()
                    when (resource) {
                        is com.dot.gallery.core.Resource.Success -> {
                            val items = resource.data ?: emptyList()
                            if (items.isEmpty()) break
                            if (items.size < SYNC_PAGE_SIZE) { page++; break }
                        }
                        is com.dot.gallery.core.Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                lastSyncError = resource.message
                            )
                            return@launch
                        }
                    }
                    page++
                }
                loadSyncStatus()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastSyncError = e.message
                )
            }
        }
    }

    companion object {
        private const val SYNC_PAGE_SIZE = 200
        private const val MAX_SYNC_PAGES = 500
    }
}
