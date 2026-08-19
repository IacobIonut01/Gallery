/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.memories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoriesUiState(
    val memories: List<MemoryInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MemoriesViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry,
    private val serverConfigDao: CloudServerConfigDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoriesUiState())
    val uiState: StateFlow<MemoriesUiState> = _uiState.asStateFlow()

    private val memoriesByAccount = mutableMapOf<Long, List<MemoryInfo>>()
    private var loadJob: Job? = null

    init {
        loadMemories()
    }

    fun loadMemories() {
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch {
            val accounts = serverConfigDao.getActive().first().filter { config ->
                val provider = registry.getByConfigId(config.id)
                provider?.isAvailable == true && ProviderCapability.MEMORIES in provider.capabilities
            }
            if (accounts.isEmpty()) {
                _uiState.value = MemoriesUiState(error = "No memories provider available")
                return@launch
            }

            memoriesByAccount.clear()
            val pendingAccounts = accounts.mapTo(mutableSetOf()) { it.id }
            accounts.forEach { config ->
                launch {
                    repository.getMemories(config.providerType, config.id).collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                memoriesByAccount[config.id] = resource.data.orEmpty()
                                pendingAccounts.remove(config.id)
                                publishMemories(pendingAccounts.isNotEmpty(), null)
                            }
                            is Resource.Error -> {
                                pendingAccounts.remove(config.id)
                                publishMemories(pendingAccounts.isNotEmpty(), resource.message)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun publishMemories(isLoading: Boolean, error: String?) {
        _uiState.value = MemoriesUiState(
            memories = memoriesByAccount.values.flatten().sortedByDescending { it.createdAt },
            isLoading = isLoading,
            error = error
        )
    }
}
