/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudArchiveViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry,
    private val serverConfigDao: CloudServerConfigDao
) : ViewModel() {

    private val _mediaState = MutableStateFlow(MediaState<Media.UriMedia>())
    val mediaState: StateFlow<MediaState<Media.UriMedia>> = _mediaState.asStateFlow()

    private val archivedByAccount = mutableMapOf<Long, List<CloudMediaEntity>>()
    private var loadJob: Job? = null

    init {
        loadArchived()
    }

    fun loadArchived() {
        loadJob?.cancel()
        _mediaState.value = _mediaState.value.copy(isLoading = true, error = "")
        loadJob = viewModelScope.launch {
            try {
                archivedByAccount.clear()
                repository.getCachedArchivedAsync()
                    .groupBy { it.serverConfigId }
                    .forEach { (configId, media) -> archivedByAccount[configId] = media }
                publishArchive(isLoading = true)

                val accounts = serverConfigDao.getActive().first().filter { config ->
                    val provider = registry.getByConfigId(config.id)
                    provider?.isAvailable == true && ProviderCapability.ARCHIVE in provider.capabilities
                }
                if (accounts.isEmpty()) {
                    publishArchive(isLoading = false)
                    return@launch
                }

                val pendingAccounts = accounts.mapTo(mutableSetOf()) { it.id }
                accounts.forEach { config ->
                    launch {
                        repository.getRemoteArchived(config.providerType, config.id).collect { resource ->
                            when (resource) {
                                is Resource.Success -> {
                                    archivedByAccount[config.id] = resource.data.orEmpty()
                                    pendingAccounts.remove(config.id)
                                    publishArchive(
                                        isLoading = pendingAccounts.isNotEmpty(),
                                        error = ""
                                    )
                                }
                                is Resource.Error -> {
                                    pendingAccounts.remove(config.id)
                                    publishArchive(
                                        isLoading = pendingAccounts.isNotEmpty(),
                                        error = resource.message.orEmpty()
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publishArchive(isLoading = false, error = e.message.orEmpty())
            }
        }
    }

    fun unarchive(media: Media.UriMedia) {
        val entity = archivedByAccount.values.flatten().firstOrNull {
            it.globalMediaId == media.id
        } ?: return
        viewModelScope.launch {
            repository.toggleArchive(
                type = entity.providerType,
                configId = entity.serverConfigId,
                remoteId = entity.remoteId,
                archived = false
            ).onSuccess {
                archivedByAccount[entity.serverConfigId] = archivedByAccount[entity.serverConfigId]
                    .orEmpty()
                    .filterNot { it.globalMediaId == entity.globalMediaId }
                publishArchive(isLoading = false)
            }
        }
    }

    private fun publishArchive(isLoading: Boolean, error: String = _mediaState.value.error) {
        val media = archivedByAccount.values
            .flatten()
            .sortedByDescending { it.takenTimestamp ?: it.timestamp }
            .map { it.toUriMedia() }
        _mediaState.value = MediaState(
            media = media,
            isLoading = isLoading,
            error = error
        )
    }
}
