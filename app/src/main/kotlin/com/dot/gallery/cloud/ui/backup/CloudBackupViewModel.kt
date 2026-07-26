/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.dao.CloudUploadPrefDao
import com.dot.gallery.cloud.di.CloudProviderInitializer
import com.dot.gallery.cloud.sync.CloudIndexProgressManager
import com.dot.gallery.cloud.sync.CloudUploadWorker
import com.dot.gallery.core.activeDataStore
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.domain.util.getUri
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject

/** Backup status for a single cloud account. */
data class AccountBackupStatus(
    val configId: Long,
    val providerType: ProviderType,
    val accountLabel: String,
    val enabledAlbumCount: Int,
    val totalAssets: Int,
    val backedUpCount: Int,
    val syncEnabled: Boolean = false,
    /**
     * Live connection state of this account's provider instance. An active, sync-capable
     * account whose provider failed to authenticate / never registered reports [ConnectionState.ERROR]
     * so the dashboard can surface it with an error indicator instead of hiding the whole
     * service (which previously made the screen fall back to the empty "Add cloud provider" state).
     */
    val connectionState: ConnectionState = ConnectionState.CONNECTED
) {
    val hasError: Boolean get() = connectionState == ConnectionState.ERROR
    val remainderCount: Int get() = (totalAssets - backedUpCount).coerceAtLeast(0)
    // Nothing selected/known yet (totalAssets == 0) is an empty bar, NOT a full one —
    // otherwise an account with no albums reads as "100% / 0 of 0 backed up".
    val progress: Float get() = if (totalAssets > 0) backedUpCount.toFloat() / totalAssets else 0f
}

data class BackupUiState(
    val totalAssets: Int = 0,
    val backedUpCount: Int = 0,
    val remainderCount: Int = 0,
    val enabledAlbumCount: Int = 0,
    /** Per-account breakdown so each cloud's backup progress is unambiguous. */
    val accounts: List<AccountBackupStatus> = emptyList(),
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
    private val configDao: CloudServerConfigDao,
    private val cloudMediaDao: CloudMediaDao,
    private val registry: ProviderRegistry,
    private val providerInitializer: CloudProviderInitializer,
    private val workManager: WorkManager,
    indexProgressManager: CloudIndexProgressManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /**
     * Whether a provider TYPE is sync-capable, resolved from a registered instance if present
     * or a transient (unauthenticated) instance otherwise. Capabilities are static per provider,
     * so this is safe to determine without a live/authenticated connection — which is exactly
     * what lets an errored-out account still be listed. Cached to avoid re-minting instances.
     */
    private val syncCapableCache = mutableMapOf<ProviderType, Boolean>()

    private fun isSyncCapable(type: ProviderType): Boolean =
        syncCapableCache.getOrPut(type) {
            (registry.getAllForType(type).firstOrNull()
                ?: providerInitializer.createTransientProvider(type)) is SyncCapableProvider
        }

    /**
     * Live connection state for an account. A missing (never-registered) provider means auth
     * failed at init, so it is reported as [ConnectionState.ERROR] rather than silently omitted.
     */
    private fun connectionStateOf(configId: Long): ConnectionState =
        registry.connectionStates.value[configId] ?: ConnectionState.ERROR

    /** Live progress of caching ("indexing") each account's remote media into the local DB. */
    val indexState: StateFlow<CloudIndexProgressManager.IndexState> = indexProgressManager.state

    val uploadPreferences = uploadPrefDao.getAll()
        .map { prefs -> prefs.associate { it.albumId to it.uploadEnabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uploadWorkRunning: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        viewModelScope.launch {
            // Track ANY backup run (periodic, "back up all", or per-account) via the shared tag.
            workManager.getWorkInfosByTagFlow(CloudUploadWorker.TAG_BACKUP)
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
        viewModelScope.launch {
            registry.connectionStates.collect { states ->
                _uiState.value = _uiState.value.copy(
                    accounts = _uiState.value.accounts.map { account ->
                        account.copy(connectionState = states[account.configId] ?: ConnectionState.ERROR)
                    }
                )
            }
        }

        // Show the full service list and last-known counts INSTANTLY from the
        // persisted snapshot — opening the dashboard must not trigger the
        // expensive scan every time. A scan only runs on first-ever open (no
        // snapshot yet), on manual refresh, or when the album selection changes.
        viewModelScope.launch { loadInitialState() }

        // Reactively track enabled album IDs so the UI refreshes immediately when
        // albums are toggled in the picker. drop(1) skips the initial emission
        // (loadInitialState already primed the UI); later changes trigger a rescan.
        viewModelScope.launch {
            uploadPrefDao.getEnabled()
                .map { prefs -> prefs.map { it.albumId }.toSet() }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    scanBackupStatus()
                }
        }
    }

    /**
     * Populates the UI from the persisted snapshot without scanning. Every active
     * sync-capable account is always listed (so the service cards render even
     * before any scan), with live album counts and last-known asset counts.
     */
    private suspend fun loadInitialState() {
        val persisted = readPersistedAccounts()
        val activeConfigs = configDao.getAll().first().filter { it.isActive }
        val accounts = activeConfigs.mapNotNull { cfg ->
            // List EVERY active sync-capable account, keyed off the persisted config rather
            // than a live provider instance. An account whose provider failed to authenticate
            // is never registered, but it must still appear here (with an error indicator) —
            // otherwise the last remaining errored account makes the whole screen collapse to
            // the empty "Add cloud provider" state.
            if (!isSyncCapable(cfg.providerType)) return@mapNotNull null
            val snapshot = persisted[cfg.id]
            AccountBackupStatus(
                configId = cfg.id,
                providerType = cfg.providerType,
                accountLabel = cfg.displayName.ifBlank { cfg.providerType.displayName },
                // Album count is a cheap DB read, so keep it live even without a scan.
                enabledAlbumCount = uploadPrefDao.getEnabledByConfigList(cfg.id).size,
                totalAssets = snapshot?.totalAssets ?: 0,
                backedUpCount = snapshot?.backedUpCount ?: 0,
                syncEnabled = cfg.syncEnabled,
                connectionState = connectionStateOf(cfg.id)
            )
        }
        publishAccounts(accounts, isScanning = false)
        // Nothing cached yet — compute once so the numbers aren't all zero.
        if (persisted.isEmpty() && accounts.isNotEmpty()) scanBackupStatus()
    }

    fun scanBackupStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanProgress = "Loading albums…")

            // Resolve every active account backed by a sync-capable provider. We list
            // ALL such accounts (even those with no albums selected yet) so the
            // dashboard surfaces every configured cloud and the user can configure
            // backup for each one. Each account is scanned independently.
            val activeConfigs = configDao.getAll().first().filter { it.isActive }
            val perConfig = activeConfigs.mapNotNull { cfg ->
                // Keep listing sync-capable accounts even when their provider is not
                // currently registered (e.g. it failed to authenticate). The live
                // provider — used only for the authoritative bulkUploadCheck — may be
                // null; in that case the account still renders with an error indicator
                // and its counts fall back to the cheap filename-cache match.
                if (!isSyncCapable(cfg.providerType)) return@mapNotNull null
                val provider = registry.getByConfigId(cfg.id) as? SyncCapableProvider
                val prefs = uploadPrefDao.getEnabledByConfigList(cfg.id)
                Triple(cfg, provider, prefs)
            }

            if (perConfig.isEmpty()) {
                publishAccounts(emptyList(), isScanning = false)
                persistAccounts(emptyList())
                return@launch
            }

            withContext(Dispatchers.IO) {
                try {
                    val hashByMediaId = HashMap<Long, String>()
                    fun hashOf(media: com.dot.gallery.feature_node.domain.model.Media): String? =
                        hashByMediaId[media.id]
                            ?: computeSha1(media)?.also { hashByMediaId[media.id] = it }

                    // Phase A — cheap, hash-free counts (filename match against the durable
                    // cache) so the service cards + hero render immediately, then Phase B
                    // refines with the authoritative hash check for the not-yet-matched
                    // remainder. Gather each account's candidate media the SAME way the
                    // upload worker does — per enabled album via getMediaByAlbumId.
                    val scanned = perConfig.map { (cfg, provider, prefs) ->
                        val media = prefs.flatMap { pref ->
                            repository.getMediaByAlbumId(pref.albumId, skipBatching = true).first().data ?: emptyList()
                        }.distinctBy { it.id }.filter { it.uri.scheme != "cloud" }
                        // Immich stores the original filename in `label` (remoteId is an
                        // opaque UUID); path-based stores key by remote path — cover both.
                        val cachedNames: Set<String> = cloudMediaDao.getByServerConfig(cfg.id).first()
                            .mapNotNull { it.label.ifBlank { it.remoteId.substringAfterLast('/') }.ifBlank { null } }
                            .toSet()
                        ScannedAccount(
                            config = cfg,
                            provider = provider,
                            connectionState = connectionStateOf(cfg.id),
                            enabledAlbumCount = prefs.size,
                            media = media,
                            cachedNames = cachedNames,
                            nameMatched = media.count { it.label in cachedNames }
                        )
                    }

                    // Phase A publish: services + provisional counts, still scanning.
                    publishAccounts(scanned.map { it.toStatus(it.nameMatched) }, isScanning = true)

                    // Phase B: only files never seen on this account need the expensive
                    // SHA-1 + bulkUploadCheck round-trip, so re-scanning a large, already
                    // synced album is near-instant instead of re-hashing every file.
                    val refined = scanned.map { s ->
                        var backedUp = s.nameMatched
                        // No live provider (errored/not authenticated) — keep the cheap
                        // filename-cache count; the authoritative network check is skipped.
                        val provider = s.provider
                        if (provider != null) {
                            val unknown = s.media.filter { it.label !in s.cachedNames }
                            if (unknown.isNotEmpty()) {
                                _uiState.value = _uiState.value.copy(scanProgress = "Checking ${unknown.size} new items…")
                            }
                            unknown.chunked(500).forEach { chunk ->
                                val present = try {
                                    provider.bulkUploadCheck(chunk.map { hashOf(it) ?: "" }).getOrDefault(emptyMap())
                                } catch (_: Exception) { emptyMap() }
                                chunk.forEachIndexed { idx, _ ->
                                    if (present[idx.toString()] == true) backedUp++
                                }
                            }
                        }
                        s.toStatus(backedUp)
                    }

                    publishAccounts(refined, isScanning = false)
                    persistAccounts(refined)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        error = e.message
                    )
                }
            }
        }
    }

    /** Per-account working set carried between the cheap and authoritative scan phases. */
    private data class ScannedAccount(
        val config: com.dot.gallery.cloud.data.entity.CloudServerConfigEntity,
        val provider: SyncCapableProvider?,
        val connectionState: ConnectionState,
        val enabledAlbumCount: Int,
        val media: List<com.dot.gallery.feature_node.domain.model.Media.UriMedia>,
        val cachedNames: Set<String>,
        val nameMatched: Int
    ) {
        fun toStatus(backedUp: Int) = AccountBackupStatus(
            configId = config.id,
            providerType = config.providerType,
            accountLabel = config.displayName.ifBlank { config.providerType.displayName },
            enabledAlbumCount = enabledAlbumCount,
            totalAssets = media.size,
            backedUpCount = backedUp,
            syncEnabled = config.syncEnabled,
            connectionState = connectionState
        )
    }

    /** Pushes an account list to the UI, recomputing the aggregate totals. */
    private fun publishAccounts(accounts: List<AccountBackupStatus>, isScanning: Boolean) {
        val total = accounts.sumOf { it.totalAssets }
        val safe = accounts.sumOf { it.backedUpCount }
        _uiState.value = _uiState.value.copy(
            accounts = accounts,
            enabledAlbumCount = accounts.sumOf { it.enabledAlbumCount },
            totalAssets = total,
            backedUpCount = safe,
            remainderCount = (total - safe).coerceAtLeast(0),
            isScanning = isScanning,
            scanProgress = if (isScanning) _uiState.value.scanProgress else ""
        )
    }

    private suspend fun readPersistedAccounts(): Map<Long, PersistedAccountStatus> {
        val raw = context.activeDataStore.data.first()[STATUS_KEY] ?: return emptyMap()
        return runCatching { statusJson.decodeFromString<List<PersistedAccountStatus>>(raw) }
            .getOrDefault(emptyList())
            .associateBy { it.configId }
    }

    private suspend fun persistAccounts(accounts: List<AccountBackupStatus>) {
        val dto = accounts.map {
            PersistedAccountStatus(
                configId = it.configId,
                totalAssets = it.totalAssets,
                backedUpCount = it.backedUpCount
            )
        }
        runCatching {
            context.activeDataStore.edit { prefs -> prefs[STATUS_KEY] = statusJson.encodeToString(dto) }
        }
    }

    /** Config IDs whose per-account manual backup is currently running/enqueued. */
    val runningAccounts: StateFlow<Set<Long>> = MutableStateFlow<Set<Long>>(emptySet()).also { flow ->
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow(CloudUploadWorker.TAG_ACCOUNT_BACKUP)
                .collect { infos ->
                    flow.value = infos
                        .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                        .flatMap { it.tags }
                        .mapNotNull { tag ->
                            tag.substringAfter("${CloudUploadWorker.TAG_ACCOUNT_BACKUP}:", "")
                                .toLongOrNull()
                        }
                        .toSet()
                }
        }
    }

    /** Triggers a manual backup. [configId] <= 0 backs up every active account. */
    fun triggerBackup(configId: Long = -1L) {
        CloudUploadWorker.triggerNow(workManager, configId)
    }

    /** Enables/disables periodic auto-sync for a single account. */
    fun setAutoSync(configId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val config = configDao.getById(configId) ?: return@launch
            configDao.update(config.copy(syncEnabled = enabled))
            scanBackupStatus()
        }
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

    /** Persisted per-account backup counts so the dashboard opens instantly without a rescan. */
    @Serializable
    private data class PersistedAccountStatus(
        val configId: Long,
        val totalAssets: Int,
        val backedUpCount: Int
    )

    private companion object {
        private val STATUS_KEY = stringPreferencesKey("cloud_backup_status_v1")
        private val statusJson = Json { ignoreUnknownKeys = true }
    }
}
