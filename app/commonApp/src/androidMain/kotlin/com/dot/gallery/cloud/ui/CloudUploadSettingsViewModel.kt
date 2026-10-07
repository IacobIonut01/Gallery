/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.work.WorkSignals
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.TaskState
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.UploadTargetResolver
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.data.dao.CloudDeleteLocalPrefDao
import com.dot.gallery.data.dao.CloudServerConfigDao
import com.dot.gallery.data.dao.CloudUploadPrefDao
import com.dot.gallery.data.entity.CloudDeleteLocalPrefEntity
import com.dot.gallery.data.entity.CloudServerConfigEntity
import com.dot.gallery.data.entity.CloudUploadPrefEntity
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.Media
import com.dot.gallery.data.repository.MediaRepository
import com.dot.gallery.domain.util.MediaOrder
import com.dot.gallery.domain.util.OrderType
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.domain.model.AllowedMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.util.or
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.core.presentation.util.update
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.sync.CloudUploadWorker
import com.dot.gallery.domain.uri.scheme
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_free_space_loading
import com.dot.gallery.generated.resources.cloud_local_deletion_unavailable
import com.dot.gallery.generated.resources.cloud_provider_not_available
import com.dot.gallery.core.util.getString


@OptIn(ExperimentalCoroutinesApi::class)
class CloudUploadSettingsViewModel @Inject constructor(
    private val context: Context,
    private val repository: MediaRepository,
    private val uploadPrefDao: CloudUploadPrefDao,
    private val deleteLocalPrefDao: CloudDeleteLocalPrefDao,
    private val configDao: CloudServerConfigDao,
    private val registry: ProviderRegistry,
    private val scheduler: BackgroundTaskScheduler,
    @me.tatarka.inject.annotations.Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * The cloud account this album picker is scoped to. Album selections made
     * here only affect this single account, so backup destinations are
     * unambiguous when multiple clouds are configured.
     */
    private val requestedConfigId: Long = savedStateHandle.get<Long>("configId") ?: -1L

    /** Resolved account id (falls back to the first active sync config when -1). */
    private val effectiveConfigId = MutableStateFlow(requestedConfigId)

    private val _accountLabel = MutableStateFlow("")
    val accountLabel: StateFlow<String> = _accountLabel.asStateFlow()

    @Volatile
    private var cachedConfig: CloudServerConfigEntity? = null

    private val _localAlbums = MutableStateFlow<List<Album>>(emptyList())
    val localAlbums: StateFlow<List<Album>> = _localAlbums.asStateFlow()

    /**
     * Whether the resolved account stores uploads by path (WebDAV/ownCloud/
     * Nextcloud/SMB/NFS). Content-addressed providers (Immich) ignore remote
     * folders, so the custom-path affordance is hidden for them.
     */
    private val _supportsUploadPaths = MutableStateFlow(false)
    val supportsUploadPaths: StateFlow<Boolean> = _supportsUploadPaths.asStateFlow()

    /** Per-album remote folder overrides, keyed by albumId. Blank = account default. */
    val customPaths: StateFlow<Map<Long, String>> = effectiveConfigId
        .flatMapLatest { id -> uploadPrefDao.getByConfig(id) }
        .map { prefs -> prefs.associate { it.albumId to it.customPath } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uploadPreferences: StateFlow<Map<Long, Boolean>> = effectiveConfigId
        .flatMapLatest { id -> uploadPrefDao.getByConfig(id) }
        .map { prefs -> prefs.associate { it.albumId to it.uploadEnabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Delete-local is now a GLOBAL per-album setting (not per-account): a local copy is removed
    // only once the asset is on EVERY cloud its album backs up to.
    val deleteLocalPreferences: StateFlow<Map<Long, Boolean>> = deleteLocalPrefDao.getAll()
        .map { prefs -> prefs.associate { it.albumId to it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadLocalAlbums()
        resolveAccount()
    }

    private fun resolveAccount() {
        viewModelScope.launch {
            val config = configDao.getById(requestedConfigId)
                ?: configDao.getActive().first()
                    .firstOrNull { it.syncEnabled }
                ?: configDao.getActive().first().firstOrNull()
            cachedConfig = config
            if (config != null) {
                effectiveConfigId.value = config.id
                _accountLabel.value = config.displayName.ifBlank { config.providerType.displayName }
                _supportsUploadPaths.value = config.providerType != ProviderType.IMMICH
            }
        }
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
            val config = cachedConfig ?: return@launch
            val existing = uploadPrefDao.get(config.id, albumId)
            val deleteLocal = deleteLocalPreferences.value[albumId] ?: false
            uploadPrefDao.upsert(
                CloudUploadPrefEntity(
                    serverConfigId = config.id,
                    albumId = albumId,
                    providerType = config.providerType,
                    albumLabel = albumLabel,
                    uploadEnabled = enabled,
                    deleteLocalAfterUpload = deleteLocal,
                    customPath = existing?.customPath.orEmpty()
                )
            )
        }
    }

    /** Sets (or clears, when blank) the per-album remote folder override. */
    fun setAlbumCustomPath(albumId: Long, customPath: String) {
        viewModelScope.launch {
            val config = cachedConfig ?: return@launch
            val existing = uploadPrefDao.get(config.id, albumId) ?: return@launch
            uploadPrefDao.upsert(
                existing.copy(customPath = UploadTargetResolver.sanitizePath(customPath))
            )
        }
    }

    fun setDeleteLocalEnabled(albumId: Long, albumLabel: String, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                deleteLocalPrefDao.upsert(
                    CloudDeleteLocalPrefEntity(albumId = albumId, enabled = true, albumLabel = albumLabel)
                )
            } else {
                deleteLocalPrefDao.delete(albumId)
            }
        }
    }

    // === Upload trigger ===

    val uploadWorkRunning: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        viewModelScope.launch {
            scheduler.observeTaskState(CloudUploadWorker.WORK_NAME_ONCE)
                .collect { state ->
                    flow.value = state is TaskState.Running || state is TaskState.Enqueued
                }
        }
    }

    fun triggerUploadNow() {
        viewModelScope.launch { CloudUploadWorker.triggerNow(scheduler) }
    }

    // === Dedup ===

    data class DedupState(
        val isScanning: Boolean = false,
        val scannedCount: Int = 0,
        val totalCount: Int = 0,
        val duplicates: List<UriMedia> = emptyList(),
        val message: String = "",
        val isDeleting: Boolean = false
    )

    private val _dedupState = MutableStateFlow(DedupState())
    val dedupState: StateFlow<DedupState> = _dedupState.asStateFlow()

    fun findDuplicates() {
        viewModelScope.launch {
            val configId = effectiveConfigId.value
            val syncProvider = registry.getByConfigId(configId) as? SyncCapableProvider
            if (syncProvider == null) {
                _dedupState.value = DedupState(
                    message = context.getString(Res.string.cloud_provider_not_available)
                )
                return@launch
            }

            _dedupState.value = DedupState(
                isScanning = true,
                message = context.getString(Res.string.cloud_free_space_loading)
            )

            withContext(Dispatchers.IO) {
                try {
                    val allMedia = repository.getMediaByType(
                        AllowedMedia.BOTH
                    ).first().data ?: emptyList()

                    val enabledPrefs = uploadPrefDao.getEnabledByConfigList(configId)
                    val prefsByAlbumId = enabledPrefs.associateBy { it.albumId }
                    val selectedAlbumIds = prefsByAlbumId.keys
                    val localMedia = allMedia.filter {
                        it.uri.scheme != "cloud" && it.albumID in selectedAlbumIds
                    }
                    _dedupState.value = _dedupState.value.copy(
                        totalCount = localMedia.size,
                        message = "Computing hashes for ${localMedia.size} items…"
                    )

                    // Compute hashes in chunks
                    val mediaWithHashes = mutableListOf<Pair<UriMedia, String>>()
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

                    val duplicateMedia = mutableListOf<UriMedia>()
                    if (syncProvider.requiresUploadChecksum) {
                        mediaWithHashes.chunked(1000).forEach { chunk ->
                            val result = syncProvider.bulkUploadCheck(chunk.map { it.second })
                                .getOrThrow()
                            duplicateMedia += verifiedItemsByIndex(chunk, result).map { it.first }
                        }
                    } else {
                        mediaWithHashes.forEach { (media, hash) ->
                            val targetPath = prefsByAlbumId[media.albumID]
                                ?.let { UploadTargetResolver.resolve(cachedConfig, it, media) }
                            val verified = try {
                                syncProvider.verifyRemoteContent(media, targetPath, hash).getOrThrow()
                            } catch (e: CancellationException) {
                                throw e
                            }
                            if (verified) duplicateMedia += media
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
                } catch (e: CancellationException) {
                    throw e
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
        _dedupState.value = _dedupState.value.copy(
            isDeleting = false,
            message = context.getString(Res.string.cloud_local_deletion_unavailable)
        )
    }

    fun clearDedupState() {
        _dedupState.value = DedupState()
    }

    private suspend fun computeSha1(media: Media): String? {
        return try {
            val uri = media.getUri()
            context.contentResolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("SHA-1")
                val buffer = ByteArray(8192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

internal fun <T> verifiedItemsByIndex(
    items: List<T>,
    verification: Map<String, Boolean>
): List<T> = items.mapIndexedNotNull { index, item ->
    item.takeIf { verification[index.toString()] == true }
}
