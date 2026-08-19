/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.space

import android.content.Context
import androidx.lifecycle.ViewModel
import com.dot.gallery.R
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudUploadPrefDao
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.domain.util.isFavorite
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
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
    // -1 = "Never": automatic/age-based removal is disabled. This is the default so
    // nothing is ever removed unless the user explicitly picks a time range.
    val cutoffDays: Int = FreeUpSpaceViewModel.NEVER_CUTOFF,
    val message: String = "",
    val error: String? = null
)

@HiltViewModel
class FreeUpSpaceViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val registry: ProviderRegistry,
    private val uploadPrefDao: CloudUploadPrefDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeUpSpaceUiState())
    val uiState: StateFlow<FreeUpSpaceUiState> = _uiState.asStateFlow()

    companion object {
        /** Sentinel cutoff meaning "never remove based on age". */
        const val NEVER_CUTOFF = -1
    }

    fun setKeepFavorites(keep: Boolean) {
        _uiState.value = _uiState.value.copy(keepFavorites = keep)
    }

    fun setCutoffDays(days: Int) {
        _uiState.value = _uiState.value.copy(cutoffDays = days)
    }

    fun scan() {
        // "Never" disables removal entirely — surface a clear message and do nothing.
        if (_uiState.value.cutoffDays == NEVER_CUTOFF) {
            _uiState.value = _uiState.value.copy(
                backedUpItems = emptyList(),
                message = context.getString(R.string.cloud_free_space_never_summary)
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isScanning = true,
            scannedCount = 0,
            message = context.getString(R.string.cloud_free_space_loading),
            backedUpItems = emptyList(),
            error = null
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val allMedia = repository.getMediaByType(AllowedMedia.BOTH)
                        .first().data.orEmpty()
                    val cutoffMs = System.currentTimeMillis() -
                            (_uiState.value.cutoffDays.toLong() * 86_400_000L)
                    val candidates = allMedia
                        .filter { it.uri.scheme != "cloud" && it.definedTimestamp * 1000L < cutoffMs }
                        .let { items ->
                            if (_uiState.value.keepFavorites) items.filterNot { it.isFavorite }
                            else items
                        }
                    val preferencesByAlbum = uploadPrefDao.getEnabledList().groupBy { it.albumId }
                    val hashCache = mutableMapOf<Long, String?>()
                    val verified = candidates.filterIndexed { index, media ->
                        val destinations = preferencesByAlbum[media.albumID].orEmpty()
                        val presentEverywhere = destinations.isNotEmpty() && destinations.all { preference ->
                            val provider = registry.getByConfigId(preference.serverConfigId)
                                    as? SyncCapableProvider ?: return@all false
                            if (provider.requiresUploadChecksum) {
                                val checksum = hashCache.getOrPut(media.id) { computeSha1(media) }
                                    ?: return@all false
                                provider.bulkUploadCheck(listOf(checksum)).getOrNull()?.get("0") == true
                            } else {
                                runCatching {
                                    provider.remoteExists(media, preference.albumLabel.takeIf { it.isNotBlank() })
                                }.getOrDefault(false)
                            }
                        }
                        _uiState.value = _uiState.value.copy(scannedCount = index + 1)
                        presentEverywhere
                    }
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        totalLocal = candidates.size,
                        backedUpItems = verified,
                        message = if (verified.isEmpty()) {
                            context.getString(R.string.cloud_free_space_none_verified)
                        } else {
                            context.getString(R.string.cloud_free_space_verified_count, verified.size)
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        error = e.message ?: context.getString(R.string.error_title)
                    )
                }
            }
        }
    }

    fun deleteLocalCopies() {
        _uiState.value = _uiState.value.copy(
            isDeleting = false,
            message = context.getString(R.string.cloud_local_deletion_unavailable)
        )
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
