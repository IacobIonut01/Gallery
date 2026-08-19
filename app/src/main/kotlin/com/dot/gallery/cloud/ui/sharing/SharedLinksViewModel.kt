/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.SharedLinkInfo
import com.dot.gallery.cloud.core.capabilities.ShareLinkCapableProvider
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

enum class SharedLinksFilter {
    ALL, ALBUMS, INDIVIDUAL
}

data class SharedLinksUiState(
    val allLinks: List<SharedLinkInfo> = emptyList(),
    val filter: SharedLinksFilter = SharedLinksFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverBaseUrls: Map<Long, String> = emptyMap(),
    val accountLabels: Map<Long, String> = emptyMap(),
    val isUpdating: Boolean = false
) {
    val filteredLinks: List<SharedLinkInfo>
        get() = when (filter) {
            SharedLinksFilter.ALL -> allLinks
            SharedLinksFilter.ALBUMS -> allLinks.filter { it.type == "ALBUM" }
            SharedLinksFilter.INDIVIDUAL -> allLinks.filter { it.type == "INDIVIDUAL" }
        }

    val hasMultipleProviders: Boolean
        get() = allLinks.map { it.serverConfigId }.distinct().size > 1
}

@HiltViewModel
class SharedLinksViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry,
    private val serverConfigDao: CloudServerConfigDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedLinksUiState())
    val uiState: StateFlow<SharedLinksUiState> = _uiState.asStateFlow()

    private val linksByAccount = mutableMapOf<Long, List<SharedLinkInfo>>()
    private var loadJob: Job? = null

    init {
        loadLinks()
    }

    fun setFilter(filter: SharedLinksFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun loadLinks() {
        loadJob?.cancel()
        linksByAccount.clear()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch {
            val accounts = serverConfigDao.getActive().first().filter { config ->
                val provider = registry.getByConfigId(config.id)
                provider is ShareLinkCapableProvider &&
                        provider.isAvailable &&
                        ProviderCapability.SHARE_MANAGE in provider.capabilities
            }
            if (accounts.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    allLinks = emptyList(),
                    isLoading = false,
                    error = "No share link provider configured"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                serverBaseUrls = accounts.associate { it.id to it.serverUrl.trimEnd('/') },
                accountLabels = accounts.associate { config ->
                    config.id to config.displayName.ifBlank { config.providerType.displayName }
                }
            )

            val pendingAccounts = accounts.mapTo(mutableSetOf()) { it.id }
            accounts.forEach { config ->
                launch {
                    repository.getSharedLinks(config.providerType, config.id).collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                linksByAccount[config.id] = resource.data.orEmpty()
                                pendingAccounts.remove(config.id)
                                publishLinks(pendingAccounts.isNotEmpty(), null)
                            }
                            is Resource.Error -> {
                                pendingAccounts.remove(config.id)
                                publishLinks(pendingAccounts.isNotEmpty(), resource.message)
                            }
                        }
                    }
                }
            }
        }
    }

    fun deleteLink(link: SharedLinkInfo) {
        viewModelScope.launch {
            repository.deleteSharedLink(
                type = link.providerType,
                configId = link.serverConfigId,
                linkId = link.id
            ).onSuccess {
                loadLinks()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateLink(
        link: SharedLinkInfo,
        description: String?,
        password: String?,
        expiresAt: Long?,
        allowDownload: Boolean,
        allowUpload: Boolean,
        showMetadata: Boolean,
        changeExpiration: Boolean
    ) {
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
                    @Suppress("UNCHECKED_CAST")
                    (updates as MutableMap<String, Any?>)["expiresAt"] = null
                }
            }
            repository.updateSharedLink(
                type = link.providerType,
                configId = link.serverConfigId,
                linkId = link.id,
                updates = updates
            ).onSuccess {
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
        val baseUrl = _uiState.value.serverBaseUrls[link.serverConfigId].orEmpty()
        return link.shareUrl(baseUrl)
    }

    fun accountLabelFor(link: SharedLinkInfo): String =
        _uiState.value.accountLabels[link.serverConfigId] ?: link.providerType.displayName

    private fun publishLinks(isLoading: Boolean, error: String?) {
        _uiState.value = _uiState.value.copy(
            allLinks = linksByAccount.values.flatten().sortedByDescending { it.createdAt },
            isLoading = isLoading,
            error = error
        )
    }
}
