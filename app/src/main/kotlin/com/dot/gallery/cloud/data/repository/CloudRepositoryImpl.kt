/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.repository

import android.net.Uri
import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudMapMarker
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.CloudTrace
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SharedLinkInfo
import com.dot.gallery.cloud.core.capabilities.MapCapableProvider
import com.dot.gallery.cloud.core.capabilities.MemoriesCapableProvider
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.ShareLinkCapableProvider
import com.dot.gallery.cloud.core.capabilities.SmartSearchCapableProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.network.ServerUrlResolver
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

internal inline fun <reified T : MediaCapabilityProvider> resolveProviderAccount(
    registry: ProviderRegistry,
    type: ProviderType,
    configId: Long,
    capabilityName: String
): Result<T> {
    val provider = registry.getByConfigId(configId)
        ?: return Result.failure(Exception("Provider account $configId not available"))
    if (provider.providerType != type) {
        return Result.failure(Exception("Provider account $configId type mismatch"))
    }
    return (provider as? T)?.let { Result.success(it) }
        ?: Result.failure(Exception("Provider account $configId does not support $capabilityName"))
}

internal fun getRemoteAlbumMediaForAccount(
    registry: ProviderRegistry,
    type: ProviderType,
    configId: Long,
    albumId: String
): Flow<Resource<List<CloudMediaEntity>>> {
    val provider = resolveProviderAccount<RemoteMediaProvider>(
        registry = registry,
        type = type,
        configId = configId,
        capabilityName = "remote albums"
    ).getOrElse { return flowOf(Resource.Error(it.message ?: "Provider account not available")) }
    return provider.getRemoteAlbumMedia(albumId)
}

@Singleton
class CloudRepositoryImpl @Inject constructor(
    private val registry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao,
    private val urlResolver: ServerUrlResolver
) : CloudRepository {

    private val _connectionStates = MutableStateFlow<Map<ProviderType, ConnectionState>>(emptyMap())
    override val connectionStates: StateFlow<Map<ProviderType, ConnectionState>> =
        _connectionStates.asStateFlow()

    private val _peopleInvalidation = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val peopleInvalidation: SharedFlow<Unit> = _peopleInvalidation.asSharedFlow()

    override val hasConfiguredProviders: Boolean
        get() = registry.getRemoteProviders().any { it.isAvailable }

    // === Server Management ===

    override suspend fun testConnection(
        type: ProviderType,
        config: CloudServerConfig
    ): Result<CloudServerInfo> {
        val provider = registry.get(type) as? RemoteMediaProvider
            ?: return Result.failure(Exception("Provider $type not available"))
        return provider.testConnection(config)
    }

    override suspend fun connect(type: ProviderType, config: CloudServerConfig): Result<Unit> {
        val provider = registry.get(type) as? RemoteMediaProvider
            ?: return Result.failure(Exception("Provider $type not available"))
        val resolved = urlResolver.resolve(config)
        registry.updateConnectionState(config.id, ConnectionState.AUTHENTICATING)
        provider.configure(resolved)
        val authResult = provider.authenticate(resolved)
        registry.updateConnectionState(config.id, provider.connectionState.value)
        return authResult.map {
            updateConnectionState(type, provider.connectionState.value)
        }
    }

    override suspend fun disconnect(type: ProviderType) {
        val provider = registry.get(type) as? RemoteMediaProvider
        if (provider != null) {
            // Call provider-specific disconnect if available
            when (provider) {
                is com.dot.gallery.cloud.core.Disconnectable -> provider.disconnect()
            }
        }
        updateConnectionState(type, ConnectionState.DISCONNECTED)
    }

    // === Aggregated Assets ===

    override fun getAllRemoteAssets(page: Int, pageSize: Int): Flow<Resource<List<CloudMediaEntity>>> {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        CloudTrace.d("Repo.getAllRemoteAssets page=$page size=$pageSize from ${providers.size} provider(s)")
        val flows = providers.map { it.getRemoteAssets(page, pageSize) }
        return combineResources(flows).onEach {
            CloudTrace.d("Repo.getAllRemoteAssets page=$page -> ${if (it is Resource.Error) "ERROR ${it.message}" else "${it.data?.size ?: 0} items"}")
        }
    }

    override fun getRemoteAssets(
        type: ProviderType,
        page: Int,
        pageSize: Int
    ): Flow<Resource<List<CloudMediaEntity>>> {
        val provider = registry.get(type) as? RemoteMediaProvider
            ?: return flowOf(Resource.Error("Provider not available"))
        CloudTrace.d("Repo.getRemoteAssets[$type] page=$page size=$pageSize")
        return provider.getRemoteAssets(page, pageSize).onEach {
            CloudTrace.d("Repo.getRemoteAssets[$type] page=$page -> ${if (it is Resource.Error) "ERROR ${it.message}" else "${it.data?.size ?: 0} items"}")
        }
    }

    override fun getRemoteFavorites(): Flow<Resource<List<CloudMediaEntity>>> {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        return combineResources(providers.map { it.getRemoteFavorites() })
    }

    override fun getRemoteTrashed(): Flow<Resource<List<CloudMediaEntity>>> {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        return combineResources(providers.map { it.getRemoteTrashed() })
    }

    // === Albums ===

    override fun getAllRemoteAlbums(): Flow<Resource<List<CloudAlbum>>> {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        CloudTrace.d("Repo.getAllRemoteAlbums from ${providers.size} provider(s)")
        val flows = providers.map { it.getRemoteAlbums() }
        return combineResources(flows).onEach {
            CloudTrace.d("Repo.getAllRemoteAlbums -> ${if (it is Resource.Error) "ERROR ${it.message}" else "${it.data?.size ?: 0} albums"}")
        }
    }

    override fun getAlbumMedia(
        type: ProviderType,
        configId: Long,
        albumId: String
    ): Flow<Resource<List<CloudMediaEntity>>> =
        getRemoteAlbumMediaForAccount(registry, type, configId, albumId).onEach {
            CloudTrace.d("Repo.getAlbumMedia[$type/$configId] '$albumId' -> ${if (it is Resource.Error) "ERROR ${it.message}" else "${it.data?.size ?: 0} items"}")
        }

    // === People ===

    override fun getAllPeople(): Flow<Resource<List<PersonInfo>>> {
        val providers = registry.getPeopleProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        return combineResources(providers.map { it.getPeople() })
    }

    override fun getPersonMedia(
        type: ProviderType,
        configId: Long,
        personId: String
    ): Flow<Resource<List<Media>>> {
        val provider = resolveProviderAccount<PeopleCapableProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "people"
        ).getOrElse { return flowOf(Resource.Error(it.message ?: "Provider account not available")) }
        return provider.getPersonMedia(personId)
    }

    // === Map Markers ===

    override fun getAllMapMarkers(): Flow<Resource<List<CloudMapMarker>>> {
        val providers = registry.getMapProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        return combineResources(providers.map { it.getMapMarkers() })
    }

    // === Smart Search ===

    override suspend fun smartSearch(query: String): Result<List<Media>> {
        val providers = registry.getSmartSearchProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return Result.success(emptyList())
        val allResults = mutableListOf<Media>()
        for (provider in providers) {
            provider.smartSearch(query).onSuccess { allResults.addAll(it) }
        }
        return Result.success(allResults)
    }

    // === Share Links ===

    override suspend fun createShareLink(
        type: ProviderType,
        configId: Long,
        assetIds: List<String>,
        expiresAt: Long?
    ): Result<String> {
        val provider = resolveProviderAccount<ShareLinkCapableProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "sharing"
        ).getOrElse { return Result.failure(it) }
        if (ProviderCapability.SHARE_CREATE !in provider.capabilities) {
            return Result.failure(Exception("Provider does not support creating share links"))
        }
        return provider.createShareLink(assetIds, expiresAt)
    }

    // === Sync ===

    override suspend fun uploadAsset(
        type: ProviderType,
        localMedia: Media,
        targetPath: String?
    ): Result<CloudMediaEntity> {
        val provider = registry.get(type) as? SyncCapableProvider
            ?: return Result.failure(Exception("Provider does not support sync"))
        return provider.uploadAsset(localMedia, targetPath)
    }

    override suspend fun downloadAsset(type: ProviderType, remoteId: String): Result<Uri> {
        val provider = registry.get(type) as? SyncCapableProvider
            ?: return Result.failure(Exception("Provider does not support sync"))
        return provider.downloadAsset(remoteId)
    }

    override suspend fun getChangedSince(
        type: ProviderType,
        timestamp: Long
    ): Result<List<CloudMediaEntity>> {
        val provider = registry.get(type) as? SyncCapableProvider
            ?: return Result.failure(Exception("Provider does not support sync"))
        return provider.getChangedSince(timestamp)
    }

    // === Search ===

    override suspend fun search(query: String): Result<List<CloudMediaEntity>> {
        val providers = registry.getRemoteProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return Result.success(emptyList())
        val allResults = mutableListOf<CloudMediaEntity>()
        for (provider in providers) {
            provider.search(query).onSuccess { allResults.addAll(it) }
        }
        return Result.success(allResults)
    }

    // === Delete ===

    override suspend fun deleteAsset(
        type: ProviderType,
        configId: Long,
        remoteId: String
    ): Result<Unit> {
        val provider = (registry.getByConfigId(configId) as? RemoteMediaProvider)
            ?.takeIf { it.providerType == type }
            ?: return Result.failure(Exception("Provider account $configId not available"))
        val result = provider.deleteAsset(remoteId)
        if (result.isSuccess) {
            // Drop from the local cache so the timeline/backup sheet update reactively.
            cloudMediaDao.delete(remoteId, type, configId)
        }
        return result
    }

    // === Archive ===

    override suspend fun toggleArchive(
        type: ProviderType,
        configId: Long,
        remoteId: String,
        archived: Boolean
    ): Result<Unit> {
        val provider = resolveProviderAccount<RemoteMediaProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "archive"
        ).getOrElse { return Result.failure(it) }
        if (ProviderCapability.ARCHIVE !in provider.capabilities) {
            return Result.failure(Exception("Provider account $configId does not support archive"))
        }
        return provider.toggleArchive(remoteId, archived)
    }

    override fun getRemoteArchived(
        type: ProviderType,
        configId: Long
    ): Flow<Resource<List<CloudMediaEntity>>> {
        val provider = resolveProviderAccount<RemoteMediaProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "archive"
        ).getOrElse { return flowOf(Resource.Error(it.message ?: "Provider account not available")) }
        if (ProviderCapability.ARCHIVE !in provider.capabilities) {
            return flowOf(Resource.Error("Provider account $configId does not support archive"))
        }
        return provider.getRemoteArchived()
    }

    override suspend fun getCachedArchivedAsync(): List<CloudMediaEntity> =
        cloudMediaDao.getArchivedAsync()

    // === Shared Links Management ===

    override fun getSharedLinks(type: ProviderType): Flow<Resource<List<SharedLinkInfo>>> {
        val providers = registry.getAllForType(type)
            .filterIsInstance<ShareLinkCapableProvider>()
            .filter {
                it.isAvailable && ProviderCapability.SHARE_MANAGE in it.capabilities
            }
        if (providers.isEmpty()) {
            return flowOf(Resource.Error("Provider does not support shared-link management"))
        }
        return combineResources(providers.map { it.getSharedLinks() })
    }

    override fun getSharedLinks(
        type: ProviderType,
        configId: Long
    ): Flow<Resource<List<SharedLinkInfo>>> {
        val provider = resolveProviderAccount<ShareLinkCapableProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "sharing"
        ).getOrElse { return flowOf(Resource.Error(it.message ?: "Provider account not available")) }
        if (ProviderCapability.SHARE_MANAGE !in provider.capabilities) {
            return flowOf(Resource.Error("Provider account $configId does not support shared-link management"))
        }
        return provider.getSharedLinks()
    }

    override suspend fun deleteSharedLink(
        type: ProviderType,
        configId: Long,
        linkId: String
    ): Result<Unit> {
        val provider = resolveProviderAccount<ShareLinkCapableProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "sharing"
        ).getOrElse { return Result.failure(it) }
        if (ProviderCapability.SHARE_MANAGE !in provider.capabilities) {
            return Result.failure(Exception("Provider account $configId does not support shared-link management"))
        }
        return provider.deleteSharedLink(linkId)
    }

    override suspend fun updateSharedLink(
        type: ProviderType,
        configId: Long,
        linkId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        val provider = resolveProviderAccount<ShareLinkCapableProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "sharing"
        ).getOrElse { return Result.failure(it) }
        if (ProviderCapability.SHARE_MANAGE !in provider.capabilities) {
            return Result.failure(Exception("Provider account $configId does not support shared-link management"))
        }
        return provider.updateSharedLink(linkId, updates)
    }

    // === People Editing ===

    override suspend fun updatePersonName(
        type: ProviderType,
        configId: Long,
        personId: String,
        name: String
    ): Result<Unit> {
        val provider = resolveProviderAccount<PeopleCapableProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "people"
        ).getOrElse { return Result.failure(it) }
        return provider.updatePersonName(personId, name).also { result ->
            if (result.isSuccess) _peopleInvalidation.tryEmit(Unit)
        }
    }

    override suspend fun updatePersonBirthDate(
        type: ProviderType,
        configId: Long,
        personId: String,
        birthDate: String
    ): Result<Unit> {
        val provider = resolveProviderAccount<PeopleCapableProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "people"
        ).getOrElse { return Result.failure(it) }
        return provider.updatePersonBirthDate(personId, birthDate)
    }

    // === Trash Bulk Operations ===

    override suspend fun emptyTrash(type: ProviderType): Result<Unit> {
        val provider = registry.get(type) as? RemoteMediaProvider
            ?: return Result.failure(Exception("Provider not available"))
        return provider.emptyTrash()
    }

    override suspend fun restoreAllTrash(type: ProviderType): Result<Unit> {
        val provider = registry.get(type) as? RemoteMediaProvider
            ?: return Result.failure(Exception("Provider not available"))
        return provider.restoreAllTrash()
    }

    // === Memories ===

    override fun getMemories(
        type: ProviderType,
        configId: Long
    ): Flow<Resource<List<MemoryInfo>>> {
        val provider = resolveProviderAccount<MemoriesCapableProvider>(
            registry = registry,
            type = type,
            configId = configId,
            capabilityName = "memories"
        ).getOrElse { return flowOf(Resource.Error(it.message ?: "Provider account not available")) }
        return provider.getMemories()
    }

    // === Cache ===

    override fun getCachedMedia(): Flow<List<CloudMediaEntity>> = cloudMediaDao.getAllForTimeline()

    override suspend fun getCachedMediaAsync(): List<CloudMediaEntity> =
        cloudMediaDao.getAllCachedAsync()

    override fun getCachedFavorites(): Flow<List<CloudMediaEntity>> =
        cloudMediaDao.getFavorites()

    override suspend fun getCachedFavoritesAsync(): List<CloudMediaEntity> =
        cloudMediaDao.getFavoritesAsync()

    override fun getCachedTrashed(): Flow<List<CloudMediaEntity>> =
        cloudMediaDao.getTrashed()

    override suspend fun getCachedTrashedAsync(): List<CloudMediaEntity> =
        cloudMediaDao.getTrashedAsync()

    override fun getCachedMediaByProvider(type: ProviderType): Flow<List<CloudMediaEntity>> =
        cloudMediaDao.getByProvider(type)

    override suspend fun clearCache(type: ProviderType) =
        cloudMediaDao.deleteByProvider(type)

    override suspend fun clearAllCache() =
        cloudMediaDao.deleteAll()

    override fun notifyProviderConnected(type: ProviderType, state: ConnectionState) {
        updateConnectionState(type, state)
    }

    // === Helpers ===

    private fun updateConnectionState(type: ProviderType, state: ConnectionState) {
        _connectionStates.value = _connectionStates.value + (type to state)
    }

    private fun <T> combineResources(flows: List<Flow<Resource<List<T>>>>): Flow<Resource<List<T>>> {
        if (flows.isEmpty()) return flowOf(Resource.Success(emptyList()))
        if (flows.size == 1) return flows[0]
        return combine(flows) { resources ->
            val allData = mutableListOf<T>()
            var hasError = false
            var errorMessage = ""
            for (resource in resources) {
                when (resource) {
                    is Resource.Success -> resource.data?.let { allData.addAll(it) }
                    is Resource.Error -> {
                        hasError = true
                        errorMessage = resource.message ?: "Unknown error"
                        resource.data?.let { allData.addAll(it) }
                    }
                }
            }
            if (hasError && allData.isEmpty()) {
                Resource.Error(errorMessage)
            } else if (hasError && allData.isNotEmpty()) {
                Resource.Error(
                    message = "Partial failure: $errorMessage",
                    data = allData
                )
            } else {
                Resource.Success(allData)
            }
        }
    }
}
