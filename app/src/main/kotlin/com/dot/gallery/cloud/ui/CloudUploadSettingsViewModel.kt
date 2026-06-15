/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudUploadPrefDao
import com.dot.gallery.cloud.data.entity.CloudUploadPrefEntity
import com.dot.gallery.cloud.sync.CloudUploadWorker
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import com.dot.gallery.feature_node.presentation.util.printDebug
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class CloudUploadSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val uploadPrefDao: CloudUploadPrefDao,
    private val registry: ProviderRegistry,
    private val workManager: WorkManager
) : ViewModel() {

    private val _localAlbums = MutableStateFlow<List<Album>>(emptyList())
    val localAlbums: StateFlow<List<Album>> = _localAlbums.asStateFlow()

    val uploadPreferences: StateFlow<Map<Long, Boolean>> = uploadPrefDao.getAll()
        .map { prefs -> prefs.associate { it.albumId to it.uploadEnabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val deleteLocalPreferences: StateFlow<Map<Long, Boolean>> = uploadPrefDao.getAll()
        .map { prefs -> prefs.associate { it.albumId to it.deleteLocalAfterUpload } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadLocalAlbums()
    }

    private fun loadLocalAlbums() {
        viewModelScope.launch {
            repository.getAlbums(MediaOrder.Label(OrderType.Ascending)).collect { resource ->
                val albums = resource.data ?: emptyList()
                // Filter out cloud albums (their URIs use the cloud:// scheme)
                _localAlbums.value = albums.filter { it.uri.scheme != "cloud" }
            }
        }
    }

    fun setAlbumUploadEnabled(albumId: Long, albumLabel: String, enabled: Boolean) {
        viewModelScope.launch {
            val existing = uploadPreferences.value
            val deleteLocal = deleteLocalPreferences.value[albumId] ?: false
            uploadPrefDao.upsert(
                CloudUploadPrefEntity(
                    albumId = albumId,
                    albumLabel = albumLabel,
                    uploadEnabled = enabled,
                    deleteLocalAfterUpload = deleteLocal
                )
            )
        }
    }

    fun setDeleteLocalEnabled(albumId: Long, albumLabel: String, enabled: Boolean) {
        viewModelScope.launch {
            val uploadEnabled = uploadPreferences.value[albumId] ?: false
            uploadPrefDao.upsert(
                CloudUploadPrefEntity(
                    albumId = albumId,
                    albumLabel = albumLabel,
                    uploadEnabled = uploadEnabled,
                    deleteLocalAfterUpload = enabled
                )
            )
        }
    }

    // === Upload trigger ===

    val uploadWorkRunning: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(CloudUploadWorker.WORK_NAME_ONCE)
                .collect { workInfos ->
                    flow.value = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                }
        }
    }

    fun triggerUploadNow() {
        CloudUploadWorker.triggerNow(workManager)
    }

    // === Dedup ===

    data class DedupState(
        val isScanning: Boolean = false,
        val scannedCount: Int = 0,
        val totalCount: Int = 0,
        val duplicates: List<Media.UriMedia> = emptyList(),
        val message: String = "",
        val isDeleting: Boolean = false
    )

    private val _dedupState = MutableStateFlow(DedupState())
    val dedupState: StateFlow<DedupState> = _dedupState.asStateFlow()

    fun findDuplicates() {
        viewModelScope.launch {
            val syncProvider = registry.getSyncProviders().firstOrNull()
            if (syncProvider == null) {
                _dedupState.value = DedupState(message = "No sync-capable provider configured")
                return@launch
            }

            _dedupState.value = DedupState(isScanning = true, message = "Loading local media…")

            withContext(Dispatchers.IO) {
                try {
                    val allMedia = repository.getMediaByType(
                        AllowedMedia.BOTH
                    ).first().data ?: emptyList()

                    // Filter out cloud media
                    val localMedia = allMedia.filter { !it.path.startsWith("Immich") && !it.path.startsWith("ownCloud") }
                    _dedupState.value = _dedupState.value.copy(
                        totalCount = localMedia.size,
                        message = "Computing hashes for ${localMedia.size} items…"
                    )

                    // Compute hashes in chunks
                    val mediaWithHashes = mutableListOf<Pair<Media.UriMedia, String>>()
                    localMedia.forEachIndexed { idx, media ->
                        val hash = computeSha1(media)
                        if (hash != null) {
                            mediaWithHashes.add(media to hash)
                        }
                        if (idx % 50 == 0) {
                            _dedupState.value = _dedupState.value.copy(
                                scannedCount = idx + 1,
                                message = "Hashing ${idx + 1}/${localMedia.size}…"
                            )
                        }
                    }

                    _dedupState.value = _dedupState.value.copy(
                        message = "Checking ${mediaWithHashes.size} items against cloud…"
                    )

                    // Check in chunks of 1000
                    val duplicateMedia = mutableListOf<Media.UriMedia>()
                    mediaWithHashes.chunked(1000).forEach { chunk ->
                        val hashes = chunk.map { it.second }
                        try {
                            val result = syncProvider.bulkUploadCheck(hashes).getOrDefault(emptyMap())
                            chunk.forEachIndexed { idx, (media, _) ->
                                val isOnCloud = result[idx.toString()] ?: false
                                if (isOnCloud) {
                                    duplicateMedia.add(media)
                                }
                            }
                        } catch (e: Exception) {
                            printDebug("Dedup: Bulk check chunk failed: ${e.message}")
                        }
                    }

                    _dedupState.value = DedupState(
                        isScanning = false,
                        scannedCount = localMedia.size,
                        totalCount = localMedia.size,
                        duplicates = duplicateMedia,
                        message = if (duplicateMedia.isEmpty()) "No duplicates found"
                        else "Found ${duplicateMedia.size} local items already on cloud"
                    )
                } catch (e: Exception) {
                    _dedupState.value = DedupState(
                        isScanning = false,
                        message = "Scan failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteLocalDuplicates() {
        val dupes = _dedupState.value.duplicates
        if (dupes.isEmpty()) return
        _dedupState.value = _dedupState.value.copy(isDeleting = true, message = "Deleting ${dupes.size} local copies…")
        viewModelScope.launch(Dispatchers.IO) {
            var deleted = 0
            dupes.forEach { media ->
                try {
                    val rows = context.contentResolver.delete(media.getUri(), null, null)
                    if (rows > 0) deleted++
                } catch (e: Exception) {
                    printDebug("Dedup: Failed to delete ${media.label}: ${e.message}")
                }
            }
            _dedupState.value = DedupState(
                message = "Deleted $deleted of ${dupes.size} local duplicates"
            )
        }
    }

    fun clearDedupState() {
        _dedupState.value = DedupState()
    }

    private fun computeSha1(media: Media): String? {
        return try {
            val uri = media.getUri()
            context.contentResolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("SHA-1")
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            null
        }
    }
}
