/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.SharedLinkInfo
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SharedLinksFilter {
    ALL, ALBUMS, INDIVIDUAL
}

data class SharedLinksUiState(
    val allLinks: List<SharedLinkInfo> = emptyList(),
    val filter: SharedLinksFilter = SharedLinksFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverBaseUrl: String = "",
    val isUpdating: Boolean = false
) {
    val filteredLinks: List<SharedLinkInfo>
        get() = when (filter) {
            SharedLinksFilter.ALL -> allLinks
            SharedLinksFilter.ALBUMS -> allLinks.filter { it.type == "ALBUM" }
            SharedLinksFilter.INDIVIDUAL -> allLinks.filter { it.type == "INDIVIDUAL" }
        }
}

@HiltViewModel
class SharedLinksViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry,
    private val serverConfigDao: CloudServerConfigDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedLinksUiState())
    val uiState: StateFlow<SharedLinksUiState> = _uiState.asStateFlow()

    init {
        loadLinks()
        loadServerBaseUrl()
    }

    private fun loadServerBaseUrl() {
        val providers = registry.getShareLinkProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        val type = providers.first().providerType
        viewModelScope.launch {
            val config = serverConfigDao.getActiveByProvider(type)
            if (config != null) {
                _uiState.value = _uiState.value.copy(
                    serverBaseUrl = config.serverUrl.trimEnd('/')
                )
            }
        }
    }

    fun setFilter(filter: SharedLinksFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun loadLinks() {
        val providers = registry.getShareLinkProviders().filter { it.isAvailable }
        if (providers.isEmpty()) {
            _uiState.value = SharedLinksUiState(error = "No share link provider configured")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true)
        val type = providers.first().providerType
        viewModelScope.launch {
            repository.getSharedLinks(type).collect { resource ->
                when (resource) {
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        allLinks = resource.data ?: emptyList(),
                        isLoading = false,
                        error = null
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = resource.message
                    )
                }
            }
        }
    }

    fun deleteLink(linkId: String) {
        val providers = registry.getShareLinkProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        val type = providers.first().providerType
        viewModelScope.launch {
            repository.deleteSharedLink(type, linkId).onSuccess {
                loadLinks()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateLink(
        linkId: String,
        description: String?,
        password: String?,
        expiresAt: Long?,
        allowDownload: Boolean,
        allowUpload: Boolean,
        showMetadata: Boolean,
        changeExpiration: Boolean
    ) {
        val providers = registry.getShareLinkProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        val type = providers.first().providerType
        _uiState.value = _uiState.value.copy(isUpdating = true)
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any>(
                "description" to (description ?: ""),
                "allowDownload" to allowDownload,
                "allowUpload" to allowUpload,
                "showMetadata" to showMetadata
            )
            if (!password.isNullOrEmpty()) {
                updates["password"] = password
            }
            if (changeExpiration) {
                if (expiresAt != null) {
                    updates["expiresAt"] = java.time.Instant.ofEpochMilli(expiresAt).toString()
                } else {
                    // Immich API accepts null to remove expiration
                    @Suppress("UNCHECKED_CAST")
                    (updates as MutableMap<String, Any?>)["expiresAt"] = null
                }
            }
            repository.updateSharedLink(type, linkId, updates).onSuccess {
                _uiState.value = _uiState.value.copy(isUpdating = false)
                loadLinks()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = e.message
                )
            }
        }
    }

    fun getShareUrl(link: SharedLinkInfo): String {
        val baseUrl = _uiState.value.serverBaseUrl
        return link.shareUrl(baseUrl)
    }
}
