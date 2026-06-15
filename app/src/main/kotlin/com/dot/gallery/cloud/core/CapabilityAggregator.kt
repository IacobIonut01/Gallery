/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import com.dot.gallery.cloud.core.capabilities.MapCapableProvider
import com.dot.gallery.cloud.core.capabilities.OcrCapableProvider
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.ShareLinkCapableProvider
import com.dot.gallery.cloud.core.capabilities.SmartSearchCapableProvider
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityAggregator @Inject constructor(
    private val registry: ProviderRegistry
) {

    fun getAllPeople(): Flow<Resource<List<PersonInfo>>> {
        val providers = registry.getPeopleProviders()
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        val flows = providers.map { it.getPeople() }
        return combineResourceFlows(flows) { lists -> lists.flatten() }
    }

    fun getAllMapMarkers(): Flow<Resource<List<CloudMapMarker>>> {
        val providers = registry.getMapProviders()
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        val flows = providers.map { it.getMapMarkers() }
        return combineResourceFlows(flows) { lists -> lists.flatten() }
    }

    fun getAllRemoteAssets(page: Int = 0, pageSize: Int = 100): Flow<Resource<List<CloudMediaEntity>>> {
        val providers = registry.getRemoteProviders()
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        val flows = providers.map { it.getRemoteAssets(page, pageSize) }
        return combineResourceFlows(flows) { lists -> lists.flatten().sortedByDescending { it.timestamp } }
    }

    fun getAllRemoteAlbums(): Flow<Resource<List<CloudAlbum>>> {
        val providers = registry.getRemoteProviders()
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        val flows = providers.map { it.getRemoteAlbums() }
        return combineResourceFlows(flows) { lists -> lists.flatten() }
    }

    fun getAllRemoteFavorites(): Flow<Resource<List<CloudMediaEntity>>> {
        val providers = registry.getRemoteProviders()
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        val flows = providers.map { it.getRemoteFavorites() }
        return combineResourceFlows(flows) { lists -> lists.flatten().sortedByDescending { it.timestamp } }
    }

    fun getAllRemoteTrashed(): Flow<Resource<List<CloudMediaEntity>>> {
        val providers = registry.getRemoteProviders()
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        val flows = providers.map { it.getRemoteTrashed() }
        return combineResourceFlows(flows) { lists -> lists.flatten().sortedByDescending { it.timestamp } }
    }

    suspend fun smartSearch(query: String): List<Pair<ProviderType, List<Media>>> {
        return registry.getSmartSearchProviders().map { provider ->
            provider.providerType to (provider.smartSearch(query).getOrElse { emptyList() })
        }
    }

    fun searchByText(query: String): Flow<Resource<List<Media>>> {
        val providers = registry.getOcrProviders()
        if (providers.isEmpty()) return flowOf(Resource.Success(emptyList()))
        val flows = providers.map { it.searchByText(query) }
        return combineResourceFlows(flows) { lists -> lists.flatten() }
    }

    suspend fun createShareLink(
        providerType: ProviderType,
        assetIds: List<String>,
        expiresAt: Long? = null
    ): Result<String> {
        val provider = registry.getShareLinkProviders()
            .firstOrNull { it.providerType == providerType }
            ?: return Result.failure(IllegalStateException("No share link provider for $providerType"))
        return provider.createShareLink(assetIds, expiresAt)
    }

    fun hasCapability(capability: ProviderCapability): Boolean =
        registry.hasCapability(capability)

    fun getActiveRemoteProviders(): List<RemoteMediaProvider> =
        registry.getRemoteProviders()

    private fun <T> combineResourceFlows(
        flows: List<Flow<Resource<List<T>>>>,
        merge: (List<List<T>>) -> List<T>
    ): Flow<Resource<List<T>>> {
        if (flows.isEmpty()) return flowOf(Resource.Success(emptyList()))
        if (flows.size == 1) return flows.first()
        return combine(flows) { resources ->
            val allData = resources.mapNotNull { it.data }
            val firstError = resources.firstOrNull { it is Resource.Error }
            if (firstError != null && allData.isEmpty()) {
                Resource.Error(firstError.message ?: "Unknown error")
            } else {
                Resource.Success(merge(allData))
            }
        }
    }
}
