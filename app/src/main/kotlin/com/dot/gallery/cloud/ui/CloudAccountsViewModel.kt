/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.CredentialEncryptor
import com.dot.gallery.cloud.core.Disconnectable
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.entity.CloudServerConfigEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloudAccountUiState(
    val configs: List<CloudServerConfigEntity> = emptyList(),
    val connectionStates: Map<Long, ConnectionState> = emptyMap(),
    val assetCounts: Map<Long, Int> = emptyMap()
)

data class AddServerUiState(
    val providerType: ProviderType = ProviderType.IMMICH,
    val serverUrl: String = "",
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val displayName: String = "",
    val syncEnabled: Boolean = false,
    val wifiOnly: Boolean = true,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false,
    val isSaving: Boolean = false,
    val savedConfigId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class CloudAccountsViewModel @Inject constructor(
    private val configDao: CloudServerConfigDao,
    private val cloudMediaDao: CloudMediaDao,
    private val registry: ProviderRegistry,
    private val credentialEncryptor: CredentialEncryptor
) : ViewModel() {

    val accountState: StateFlow<List<CloudServerConfigEntity>> = configDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _addServerState = MutableStateFlow(AddServerUiState())
    val addServerState: StateFlow<AddServerUiState> = _addServerState.asStateFlow()

    fun initAddServer(providerType: ProviderType) {
        _addServerState.value = AddServerUiState(providerType = providerType)
    }

    fun initEditServer(configId: Long) {
        viewModelScope.launch {
            configDao.getById(configId)?.let { entity ->
                _addServerState.value = AddServerUiState(
                    providerType = entity.providerType,
                    serverUrl = entity.serverUrl,
                    apiKey = entity.apiKey?.let { credentialEncryptor.decrypt(it) } ?: "",
                    username = entity.username ?: "",
                    password = entity.encryptedPassword?.let { credentialEncryptor.decrypt(it) } ?: "",
                    displayName = entity.displayName,
                    syncEnabled = entity.syncEnabled,
                    wifiOnly = entity.wifiOnly,
                    savedConfigId = entity.id
                )
            }
        }
    }

    fun updateServerUrl(url: String) {
        _addServerState.value = _addServerState.value.copy(serverUrl = url, testResult = null)
    }

    fun updateApiKey(key: String) {
        _addServerState.value = _addServerState.value.copy(apiKey = key, testResult = null)
    }

    fun updateUsername(username: String) {
        _addServerState.value = _addServerState.value.copy(username = username, testResult = null)
    }

    fun updatePassword(password: String) {
        _addServerState.value = _addServerState.value.copy(password = password, testResult = null)
    }

    fun updateDisplayName(name: String) {
        _addServerState.value = _addServerState.value.copy(displayName = name)
    }

    fun updateSyncEnabled(enabled: Boolean) {
        _addServerState.value = _addServerState.value.copy(syncEnabled = enabled)
    }

    fun updateWifiOnly(wifiOnly: Boolean) {
        _addServerState.value = _addServerState.value.copy(wifiOnly = wifiOnly)
    }

    fun testConnection() {
        val state = _addServerState.value
        if (state.serverUrl.isBlank()) {
            _addServerState.value = state.copy(testResult = "Server URL is required", testSuccess = false)
            return
        }
        _addServerState.value = state.copy(isTesting = true, testResult = null)
        viewModelScope.launch {
            try {
                val config = buildConfig(state)
                val provider = registry.get(state.providerType) as? RemoteMediaProvider
                if (provider != null) {
                    // Ensure provider is configured with the test URL before testing
                    provider.configure(config)
                    val result = provider.testConnection(config)
                    result.fold(
                        onSuccess = { info ->
                            _addServerState.value = _addServerState.value.copy(
                                isTesting = false,
                                testResult = info.serverName,
                                testSuccess = true
                            )
                        },
                        onFailure = { e ->
                            _addServerState.value = _addServerState.value.copy(
                                isTesting = false,
                                testResult = e.message ?: "Connection failed",
                                testSuccess = false
                            )
                        }
                    )
                } else {
                    _addServerState.value = _addServerState.value.copy(
                        isTesting = false,
                        testResult = "Provider not available. Is it enabled in build?",
                        testSuccess = false
                    )
                }
            } catch (e: Exception) {
                _addServerState.value = _addServerState.value.copy(
                    isTesting = false,
                    testResult = e.message ?: "Unknown error",
                    testSuccess = false
                )
            }
        }
    }

    private val _syncCompleted = MutableStateFlow(false)
    val syncCompleted: StateFlow<Boolean> = _syncCompleted.asStateFlow()

    fun saveServer() {
        val state = _addServerState.value
        _addServerState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val encryptedApiKey = state.apiKey.ifBlank { null }?.let {
                    credentialEncryptor.encrypt(it)
                }
                val encryptedPassword = state.password.ifBlank { null }?.let {
                    credentialEncryptor.encrypt(it)
                }
                val entity = CloudServerConfigEntity(
                    id = state.savedConfigId ?: 0L,
                    providerType = state.providerType,
                    serverUrl = state.serverUrl.trimEnd('/'),
                    apiKey = encryptedApiKey,
                    username = state.username.ifBlank { null },
                    encryptedPassword = encryptedPassword,
                    displayName = state.displayName.ifBlank {
                        "${state.providerType.displayName} Server"
                    },
                    isActive = true,
                    syncEnabled = state.syncEnabled,
                    wifiOnly = state.wifiOnly
                )
                val id = configDao.insert(entity)

                // Configure and authenticate the provider with plaintext credentials
                val config = buildConfig(state.copy(savedConfigId = id))
                val provider = registry.get(state.providerType) as? RemoteMediaProvider
                provider?.configure(config)
                provider?.authenticate(config)

                _addServerState.value = _addServerState.value.copy(
                    isSaving = false,
                    savedConfigId = id
                )

                // Trigger initial sync (media + albums) after successful save
                triggerSync(state.providerType)

                _syncCompleted.value = true
            } catch (e: Exception) {
                _addServerState.value = _addServerState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Save failed"
                )
            }
        }
    }

    fun deleteServer(configId: Long) {
        viewModelScope.launch {
            val entity = configDao.getById(configId) ?: return@launch
            cloudMediaDao.deleteByServerConfig(configId)
            configDao.deleteById(configId)
            val provider = registry.get(entity.providerType)
            if (provider is Disconnectable) {
                provider.disconnect()
            }
        }
    }

    fun getConnectionState(providerType: ProviderType): ConnectionState {
        val provider = registry.get(providerType) as? RemoteMediaProvider
        return provider?.connectionState?.value ?: ConnectionState.DISCONNECTED
    }

    fun updateConfigById(configId: Long, transform: CloudServerConfigEntity.() -> CloudServerConfigEntity) {
        viewModelScope.launch {
            val entity = configDao.getById(configId) ?: return@launch
            val updated = entity.transform()
            configDao.update(updated)
        }
    }

    suspend fun getAssetCount(providerType: ProviderType): Int {
        return cloudMediaDao.countByProvider(providerType)
    }

    private val _storageInfo = MutableStateFlow<Map<ProviderType, CloudStorageInfo>>(emptyMap())
    val storageInfo: StateFlow<Map<ProviderType, CloudStorageInfo>> = _storageInfo.asStateFlow()

    private val _assetCounts = MutableStateFlow<Map<ProviderType, Int>>(emptyMap())
    val assetCounts: StateFlow<Map<ProviderType, Int>> = _assetCounts.asStateFlow()

    private val _serverVersions = MutableStateFlow<Map<ProviderType, String>>(emptyMap())
    val serverVersions: StateFlow<Map<ProviderType, String>> = _serverVersions.asStateFlow()

    fun loadStorageInfo() {
        viewModelScope.launch {
            val providers = registry.getRemoteProviders()
            providers.forEach { provider ->
                try {
                    val result = provider.getStorageInfo()
                    result.onSuccess { info ->
                        _storageInfo.value = _storageInfo.value + (provider.providerType to info)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun loadAssetCounts() {
        viewModelScope.launch {
            val configs = configDao.getAll().first()
            configs.forEach { config ->
                val count = cloudMediaDao.countByProvider(config.providerType)
                _assetCounts.value = _assetCounts.value + (config.providerType to count)
            }
        }
    }

    fun loadServerVersions() {
        viewModelScope.launch {
            val providers = registry.getRemoteProviders()
            providers.forEach { provider ->
                try {
                    provider.getServerVersion().onSuccess { version ->
                        _serverVersions.value = _serverVersions.value + (provider.providerType to version)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    private val _syncProgress = MutableStateFlow<Map<ProviderType, SyncProgress>>(emptyMap())
    val syncProgress: StateFlow<Map<ProviderType, SyncProgress>> = _syncProgress.asStateFlow()

    data class SyncProgress(
        val isSyncing: Boolean = false,
        val mediaCount: Int = 0,
        val albumCount: Int = 0,
        val message: String = ""
    )

    fun triggerSync(providerType: ProviderType) {
        viewModelScope.launch {
            val provider = registry.get(providerType) as? RemoteMediaProvider ?: return@launch
            _syncProgress.value = _syncProgress.value + (providerType to SyncProgress(
                isSyncing = true, message = "Fetching media..."
            ))
            try {
                var mediaCount = 0
                provider.getRemoteAssets(0, 500).collect { resource ->
                    if (resource is com.dot.gallery.core.Resource.Success) {
                        mediaCount = resource.data?.size ?: 0
                        _syncProgress.value = _syncProgress.value + (providerType to SyncProgress(
                            isSyncing = true, mediaCount = mediaCount, message = "Synced $mediaCount media items..."
                        ))
                    }
                }
                _syncProgress.value = _syncProgress.value + (providerType to SyncProgress(
                    isSyncing = true, mediaCount = mediaCount, message = "Fetching albums..."
                ))
                var albumCount = 0
                provider.getRemoteAlbums().collect { resource ->
                    if (resource is com.dot.gallery.core.Resource.Success) {
                        albumCount = resource.data?.size ?: 0
                    }
                }
                _syncProgress.value = _syncProgress.value + (providerType to SyncProgress(
                    isSyncing = false, mediaCount = mediaCount, albumCount = albumCount,
                    message = "Done: $mediaCount media, $albumCount albums"
                ))
            } catch (e: Exception) {
                _syncProgress.value = _syncProgress.value + (providerType to SyncProgress(
                    isSyncing = false, message = "Sync failed: ${e.message}"
                ))
            }
        }
    }

    private fun buildConfig(state: AddServerUiState) = CloudServerConfig(
        id = state.savedConfigId ?: 0L,
        providerType = state.providerType,
        serverUrl = state.serverUrl.trimEnd('/'),
        apiKey = state.apiKey.ifBlank { null },
        username = state.username.ifBlank { null },
        password = state.password.ifBlank { null },
        displayName = state.displayName,
        syncEnabled = state.syncEnabled,
        wifiOnly = state.wifiOnly
    )
}
