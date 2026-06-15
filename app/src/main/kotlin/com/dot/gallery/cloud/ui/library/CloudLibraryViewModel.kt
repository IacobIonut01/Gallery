/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SharedLinkInfo
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloudLibraryUiState(
    val favoriteCount: Int = 0,
    val archivedCount: Int = 0,
    val trashedCount: Int = 0,
    val totalCloudCount: Int = 0,
    val people: List<PersonInfo> = emptyList(),
    val sharedLinks: List<SharedLinkInfo> = emptyList(),
    val memories: List<MemoryInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasArchiveCapability: Boolean = false,
    val hasMemoriesCapability: Boolean = false,
    val hasShareLinkCapability: Boolean = false,
    val hasPeopleCapability: Boolean = false,
    val hasMapCapability: Boolean = false
)

@HiltViewModel
class CloudLibraryViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudLibraryUiState())
    val uiState: StateFlow<CloudLibraryUiState> = _uiState.asStateFlow()

    val hasConfiguredProviders: Boolean
        get() = repository.hasConfiguredProviders

    init {
        refreshCapabilities()
        if (hasConfiguredProviders) {
            loadCounts()
            loadPeople()
            loadSharedLinks()
            loadMemories()
        }
    }

    private fun refreshCapabilities() {
        val providers = registry.getRemoteProviders()
        val allCaps = providers.flatMap { it.capabilities }.toSet()
        _uiState.value = _uiState.value.copy(
            hasArchiveCapability = ProviderCapability.ARCHIVE in allCaps,
            hasMemoriesCapability = ProviderCapability.MEMORIES in allCaps,
            hasShareLinkCapability = ProviderCapability.SHARE_LINK in allCaps,
            hasPeopleCapability = ProviderCapability.PEOPLE in allCaps,
            hasMapCapability = ProviderCapability.MAP in allCaps
        )
    }

    fun loadCounts() {
        viewModelScope.launch {
            try {
                val favCount = cloudMediaDao.countFavorites()
                val archiveCount = cloudMediaDao.countArchived()
                val trashCount = cloudMediaDao.countTrashed()
                val totalCount = cloudMediaDao.countCached()
                _uiState.value = _uiState.value.copy(
                    favoriteCount = favCount,
                    archivedCount = archiveCount,
                    trashedCount = trashCount,
                    totalCloudCount = totalCount
                )
            } catch (_: Exception) { }
        }
    }

    private fun loadPeople() {
        viewModelScope.launch {
            repository.getAllPeople().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            people = resource.data ?: emptyList()
                        )
                    }
                    is Resource.Error -> { }
                }
            }
        }
    }

    private fun loadSharedLinks() {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        viewModelScope.launch {
            repository.getSharedLinks(providers.first().providerType).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            sharedLinks = resource.data ?: emptyList()
                        )
                    }
                    is Resource.Error -> { }
                }
            }
        }
    }

    private fun loadMemories() {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        viewModelScope.launch {
            repository.getMemories(providers.first().providerType).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            memories = resource.data ?: emptyList()
                        )
                    }
                    is Resource.Error -> { }
                }
            }
        }
    }

    fun refresh() {
        refreshCapabilities()
        loadCounts()
        loadPeople()
        loadSharedLinks()
        loadMemories()
    }
}
