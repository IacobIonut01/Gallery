/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudUploadPrefDao
import com.dot.gallery.cloud.sync.CloudUploadWorker
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject

data class BackupUiState(
    val totalAssets: Int = 0,
    val backedUpCount: Int = 0,
    val remainderCount: Int = 0,
    val enabledAlbumCount: Int = 0,
    val isScanning: Boolean = false,
    val isUploading: Boolean = false,
    val scanProgress: String = "",
    val error: String? = null
)

@HiltViewModel
class CloudBackupViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val uploadPrefDao: CloudUploadPrefDao,
    private val cloudMediaDao: CloudMediaDao,
    private val registry: ProviderRegistry,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    val uploadPreferences = uploadPrefDao.getAll()
        .map { prefs -> prefs.associate { it.albumId to it.uploadEnabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uploadWorkRunning: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(CloudUploadWorker.WORK_NAME_ONCE)
                .collect { workInfos ->
                    val running = workInfos.any {
                        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                    }
                    flow.value = running
                    _uiState.value = _uiState.value.copy(isUploading = running)
                }
        }
    }

    init {
        // Reactively track enabled album IDs so the UI refreshes
        // immediately when albums are toggled in the picker screen.
        // drop(1) skips the initial emission since scanBackupStatus()
        // already runs below; subsequent changes trigger a full rescan.
        viewModelScope.launch {
            uploadPrefDao.getEnabled()
                .map { prefs -> prefs.map { it.albumId }.toSet() }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    scanBackupStatus()
                }
        }
        scanBackupStatus()
    }

    fun scanBackupStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanProgress = "Loading albums…")

            val enabledPrefs = uploadPrefDao.getEnabledList()
            _uiState.value = _uiState.value.copy(enabledAlbumCount = enabledPrefs.size)

            if (enabledPrefs.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanProgress = "",
                    totalAssets = 0,
                    backedUpCount = 0,
                    remainderCount = 0
                )
                return@launch
            }

            val syncProvider = registry.getSyncProviders().firstOrNull()
            if (syncProvider == null) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = "No sync-capable provider"
                )
                return@launch
            }

            withContext(Dispatchers.IO) {
                try {
                    val enabledAlbumIds = enabledPrefs.map { it.albumId }.toSet()
                    val allMedia = repository.getMediaByType(AllowedMedia.BOTH)
                        .first().data ?: emptyList()
                    val localMedia = allMedia.filter {
                        it.albumID in enabledAlbumIds && it.uri.scheme != "cloud"
                    }

                    _uiState.value = _uiState.value.copy(
                        totalAssets = localMedia.size,
                        scanProgress = "Checking ${localMedia.size} items…"
                    )

                    var backedUp = 0
                    localMedia.chunked(500).forEach { chunk ->
                        val hashes = chunk.mapNotNull { computeSha1(it) }
                        try {
                            val result = syncProvider.bulkUploadCheck(hashes)
                                .getOrDefault(emptyMap())
                            backedUp += result.values.count { it }
                        } catch (_: Exception) { }
                    }

                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        backedUpCount = backedUp,
                        remainderCount = localMedia.size - backedUp,
                        scanProgress = ""
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun triggerBackup() {
        CloudUploadWorker.triggerNow(workManager)
    }

    private fun computeSha1(media: com.dot.gallery.feature_node.domain.model.Media): String? {
        return try {
            context.contentResolver.openInputStream(media.getUri())?.use { input ->
                val digest = MessageDigest.getInstance("SHA-1")
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (_: Exception) { null }
    }
}
