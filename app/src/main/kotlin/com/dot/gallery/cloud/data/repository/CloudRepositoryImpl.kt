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
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.cloud.core.PersonInfo
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRepositoryImpl @Inject constructor(
    private val registry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao
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
        provider.configure(config)
        val authResult = provider.authenticate(config)
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
        val flows = providers.map { it.getRemoteAssets(page, pageSize) }
        return combineResources(flows)
    }

    override fun getRemoteAssets(
        type: ProviderType,
        page: Int,
        pageSize: Int
    ): Flow<Resource<List<CloudMediaEntity>>> {
        val provider = registry.get(type) as? RemoteMediaProvider
            ?: return flowOf(Resource.Error("Provider not available"))
        return provider.getRemoteAssets(page, pageSize)
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
        val flows = providers.map { it.getRemoteAlbums() }
        return combineResources(flows)
    }

    override fun getAlbumMedia(
        type: ProviderType,
        albumId: String
    ): Flow<Resource<List<CloudMediaEntity>>> {
        val provider = registry.get(type) as? RemoteMediaProvider
            ?: return flowOf(Resource.Error("Provider not available"))
        return provider.getRemoteAlbumMedia(albumId)
    }

    // === People ===

    override fun getAllPeople(): Flow<Resource<List<PersonInfo>>> {
        val providers = registry.getPeopleProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        return combineResources(providers.map { it.getPeople() })
    }

    override fun getPersonMedia(
        type: ProviderType,
        personId: String
    ): Flow<Resource<List<Media>>> {
        val provider = registry.get(type) as? PeopleCapableProvider
            ?: return flowOf(Resource.Error("Provider not available"))
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
        assetIds: List<String>,
        expiresAt: Long?
    ): Result<String> {
        val provider = registry.get(type) as? ShareLinkCapableProvider
            ?: return Result.failure(Exception("Provider does not support sharing"))
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

    // === Archive ===

    override suspend fun toggleArchive(
        type: ProviderType,
        remoteId: String,
        archived: Boolean
    ): Result<Unit> {
        val provider = registry.get(type) as? RemoteMediaProvider
            ?: return Result.failure(Exception("Provider not available"))
        return provider.toggleArchive(remoteId, archived)
    }

    override fun getRemoteArchived(type: ProviderType): Flow<Resource<List<CloudMediaEntity>>> {
        val provider = registry.get(type) as? RemoteMediaProvider
            ?: return flowOf(Resource.Error("Provider not available"))
        return provider.getRemoteArchived()
    }

    override suspend fun getCachedArchivedAsync(): List<CloudMediaEntity> =
        cloudMediaDao.getArchivedAsync()

    // === Shared Links Management ===

    override fun getSharedLinks(type: ProviderType): Flow<Resource<List<SharedLinkInfo>>> {
        val provider = registry.get(type) as? ShareLinkCapableProvider
            ?: return flowOf(Resource.Error("Provider does not support sharing"))
        return provider.getSharedLinks()
    }

    override suspend fun deleteSharedLink(type: ProviderType, linkId: String): Result<Unit> {
        val provider = registry.get(type) as? ShareLinkCapableProvider
            ?: return Result.failure(Exception("Provider does not support sharing"))
        return provider.deleteSharedLink(linkId)
    }

    override suspend fun updateSharedLink(
        type: ProviderType,
        linkId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        val provider = registry.get(type) as? ShareLinkCapableProvider
            ?: return Result.failure(Exception("Provider does not support sharing"))
        return provider.updateSharedLink(linkId, updates)
    }

    // === People Editing ===

    override suspend fun updatePersonName(
        type: ProviderType,
        personId: String,
        name: String
    ): Result<Unit> {
        val provider = registry.get(type) as? PeopleCapableProvider
            ?: return Result.failure(Exception("Provider does not support people"))
        return provider.updatePersonName(personId, name).also { result ->
            if (result.isSuccess) _peopleInvalidation.tryEmit(Unit)
        }
    }

    override suspend fun updatePersonBirthDate(
        type: ProviderType,
        personId: String,
        birthDate: String
    ): Result<Unit> {
        val provider = registry.get(type) as? PeopleCapableProvider
            ?: return Result.failure(Exception("Provider does not support people"))
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

    override fun getMemories(type: ProviderType): Flow<Resource<List<MemoryInfo>>> {
        val provider = registry.get(type) as? MemoriesCapableProvider
            ?: return flowOf(Resource.Error("Provider does not support memories"))
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
