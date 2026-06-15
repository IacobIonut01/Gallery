/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.space

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.domain.util.isFavorite
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import com.dot.gallery.feature_node.presentation.util.printDebug
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject

data class FreeUpSpaceUiState(
    val isScanning: Boolean = false,
    val isDeleting: Boolean = false,
    val scannedCount: Int = 0,
    val totalLocal: Int = 0,
    val backedUpItems: List<Media.UriMedia> = emptyList(),
    val deletedCount: Int = 0,
    val keepFavorites: Boolean = true,
    val cutoffDays: Int = 30,
    val message: String = "",
    val error: String? = null
)

@HiltViewModel
class FreeUpSpaceViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val registry: ProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeUpSpaceUiState())
    val uiState: StateFlow<FreeUpSpaceUiState> = _uiState.asStateFlow()

    fun setKeepFavorites(keep: Boolean) {
        _uiState.value = _uiState.value.copy(keepFavorites = keep)
    }

    fun setCutoffDays(days: Int) {
        _uiState.value = _uiState.value.copy(cutoffDays = days)
    }

    fun scan() {
        val syncProvider = registry.getSyncProviders().firstOrNull()
        if (syncProvider == null) {
            _uiState.value = _uiState.value.copy(error = "No sync provider")
            return
        }

        _uiState.value = _uiState.value.copy(isScanning = true, message = "Loading local media…", backedUpItems = emptyList())
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val allMedia = repository.getMediaByType(AllowedMedia.BOTH)
                        .first().data ?: emptyList()
                    val localMedia = allMedia.filter { it.uri.scheme != "cloud" }

                    val cutoffMs = System.currentTimeMillis() - (_uiState.value.cutoffDays.toLong() * 86400000L)
                    val candidates = localMedia.filter { it.timestamp < cutoffMs }
                        .let { list ->
                            if (_uiState.value.keepFavorites) list.filter { !it.isFavorite }
                            else list
                        }

                    _uiState.value = _uiState.value.copy(
                        totalLocal = candidates.size,
                        message = "Checking ${candidates.size} items against cloud…"
                    )

                    val backedUp = mutableListOf<Media.UriMedia>()
                    candidates.chunked(500).forEach { chunk ->
                        val hashes = chunk.mapNotNull { computeSha1(it) }
                        try {
                            val result = syncProvider.bulkUploadCheck(hashes).getOrDefault(emptyMap())
                            chunk.forEachIndexed { idx, media ->
                                if (result[idx.toString()] == true) {
                                    backedUp.add(media)
                                }
                            }
                        } catch (_: Exception) { }
                        _uiState.value = _uiState.value.copy(
                            scannedCount = _uiState.value.scannedCount + chunk.size
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        backedUpItems = backedUp,
                        message = if (backedUp.isEmpty()) "No backed-up items older than ${_uiState.value.cutoffDays} days found"
                        else "Found ${backedUp.size} items safe to remove"
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isScanning = false, error = e.message)
                }
            }
        }
    }

    fun deleteLocalCopies() {
        val items = _uiState.value.backedUpItems
        if (items.isEmpty()) return
        _uiState.value = _uiState.value.copy(isDeleting = true, message = "Removing ${items.size} local copies…")
        viewModelScope.launch(Dispatchers.IO) {
            var deleted = 0
            items.forEach { media ->
                try {
                    val rows = context.contentResolver.delete(media.getUri(), null, null)
                    if (rows > 0) deleted++
                } catch (e: Exception) {
                    printDebug("FreeUpSpace: Failed to delete ${media.label}: ${e.message}")
                }
            }
            _uiState.value = _uiState.value.copy(
                isDeleting = false,
                deletedCount = deleted,
                backedUpItems = emptyList(),
                message = "Freed up $deleted items"
            )
        }
    }

    private fun computeSha1(media: Media): String? {
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
