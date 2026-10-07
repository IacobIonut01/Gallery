/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.destinations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.data.dao.CloudDeleteLocalPrefDao
import com.dot.gallery.data.dao.CloudServerConfigDao
import com.dot.gallery.data.dao.CloudUploadPrefDao
import com.dot.gallery.data.entity.CloudDeleteLocalPrefEntity
import com.dot.gallery.data.entity.CloudUploadPrefEntity
import com.dot.gallery.domain.model.Album
import com.dot.gallery.data.repository.MediaRepository
import com.dot.gallery.domain.util.MediaOrder
import com.dot.gallery.domain.util.OrderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.dot.gallery.core.util.Column
import com.dot.gallery.domain.uri.scheme


/** A single cloud account presented as a backup destination column. */
data class DestinationAccount(
    val configId: Long,
    val providerType: ProviderType,
    val label: String
)

/**
 * Backing view model for the Destinations matrix: the album × cloud fan-out hub.
 *
 * Each cell maps to a row in [CloudUploadPrefEntity] keyed by `(serverConfigId, albumId)`,
 * so the same local album can target several clouds at once (redundancy). No schema
 * change is required — this screen is a cross-account view over the existing per-account
 * upload preferences that the picker and [com.dot.gallery.cloud.sync.CloudUploadWorker]
 * already consume.
 */
class CloudDestinationsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val configDao: CloudServerConfigDao,
    private val uploadPrefDao: CloudUploadPrefDao,
    private val deleteLocalPrefDao: CloudDeleteLocalPrefDao,
    @me.tatarka.inject.annotations.Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * When opened from a specific service's "Select albums" action, the screen is
     * scoped to that one account (acting as its album picker). <= 0 means show the
     * full album × cloud matrix.
     */
    val filterConfigId: Long = savedStateHandle.get<Long>("configId") ?: -1L

    /** On-device albums (folders) offered as backup sources, excluding cloud albums. */
    private val _localAlbums = MutableStateFlow<List<Album>>(emptyList())
    val localAlbums: StateFlow<List<Album>> = _localAlbums.asStateFlow()

    /** Active accounts shown as destination columns. */
    val accounts: StateFlow<List<DestinationAccount>> = configDao.getAll()
        .map { configs ->
            configs.filter { it.isActive }
                .map {
                    DestinationAccount(
                        configId = it.id,
                        providerType = it.providerType,
                        label = it.displayName.ifBlank { it.providerType.displayName }
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Full preference set, used both for cell state and to preserve delete-local on toggle. */
    private val allPrefs: StateFlow<List<CloudUploadPrefEntity>> = uploadPrefDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** albumId -> set of configIds the album is enabled for. Drives the cell toggles. */
    val enabledByAlbum: StateFlow<Map<Long, Set<Long>>> = uploadPrefDao.getAll()
        .map { prefs ->
            prefs.filter { it.uploadEnabled }
                .groupBy { it.albumId }
                .mapValues { (_, rows) -> rows.map { it.serverConfigId }.toSet() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** albumId -> whether local copies are removed once backed up to ALL the album's clouds. */
    val deleteLocalByAlbum: StateFlow<Map<Long, Boolean>> = deleteLocalPrefDao.getAll()
        .map { prefs -> prefs.filter { it.enabled }.associate { it.albumId to true } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadLocalAlbums()
    }

    /** Toggles the global per-album "delete from device after backup" setting. */
    fun setDeleteLocal(album: Album, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                deleteLocalPrefDao.upsert(
                    CloudDeleteLocalPrefEntity(albumId = album.id, enabled = true, albumLabel = album.label)
                )
            } else {
                deleteLocalPrefDao.delete(album.id)
            }
        }
    }

    private fun loadLocalAlbums() {
        viewModelScope.launch {
            repository.getAlbums(MediaOrder.Label(OrderType.Ascending)).collect { resource ->
                _localAlbums.value = (resource.data ?: emptyList())
                    .filter { it.uri.scheme != "cloud" }
            }
        }
    }

    /** Enables/disables a single (album → account) destination cell. */
    fun setDestination(album: Album, configId: Long, enabled: Boolean) {
        viewModelScope.launch { applyDestination(album, configId, enabled) }
    }

    /** Fan-out helper: toggle one album across every configured cloud. */
    fun setAlbumAllClouds(album: Album, enabled: Boolean) {
        viewModelScope.launch {
            accounts.value.forEach { applyDestination(album, it.configId, enabled) }
        }
    }

    /** Column helper: toggle one cloud across every local album. */
    fun setCloudAllAlbums(configId: Long, enabled: Boolean) {
        viewModelScope.launch {
            _localAlbums.value.forEach { applyDestination(it, configId, enabled) }
        }
    }

    private suspend fun applyDestination(album: Album, configId: Long, enabled: Boolean) {
        val config = configDao.getById(configId) ?: return
        // Preserve any existing delete-local choice for this cell.
        val deleteLocal = allPrefs.value
            .firstOrNull { it.serverConfigId == configId && it.albumId == album.id }
            ?.deleteLocalAfterUpload ?: false
        uploadPrefDao.upsert(
            CloudUploadPrefEntity(
                serverConfigId = configId,
                albumId = album.id,
                providerType = config.providerType,
                albumLabel = album.label,
                uploadEnabled = enabled,
                deleteLocalAfterUpload = deleteLocal
            )
        )
    }
}
