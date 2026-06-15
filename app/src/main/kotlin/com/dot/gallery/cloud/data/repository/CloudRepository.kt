/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.repository

import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudMapMarker
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SharedLinkInfo
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface CloudRepository {

    val hasConfiguredProviders: Boolean
    val connectionStates: StateFlow<Map<ProviderType, ConnectionState>>
    val peopleInvalidation: SharedFlow<Unit>

    // Server management
    suspend fun testConnection(type: ProviderType, config: CloudServerConfig): Result<CloudServerInfo>
    suspend fun connect(type: ProviderType, config: CloudServerConfig): Result<Unit>
    suspend fun disconnect(type: ProviderType)
    fun notifyProviderConnected(type: ProviderType, state: ConnectionState)

    // Aggregated assets from all providers
    fun getAllRemoteAssets(page: Int = 0, pageSize: Int = 100): Flow<Resource<List<CloudMediaEntity>>>
    fun getRemoteAssets(type: ProviderType, page: Int = 0, pageSize: Int = 100): Flow<Resource<List<CloudMediaEntity>>>
    fun getRemoteFavorites(): Flow<Resource<List<CloudMediaEntity>>>
    fun getRemoteTrashed(): Flow<Resource<List<CloudMediaEntity>>>

    // Albums from all providers
    fun getAllRemoteAlbums(): Flow<Resource<List<CloudAlbum>>>
    fun getAlbumMedia(type: ProviderType, albumId: String): Flow<Resource<List<CloudMediaEntity>>>

    // People from all providers
    fun getAllPeople(): Flow<Resource<List<PersonInfo>>>
    fun getPersonMedia(type: ProviderType, personId: String): Flow<Resource<List<Media>>>

    // Map markers from all providers
    fun getAllMapMarkers(): Flow<Resource<List<CloudMapMarker>>>

    // Smart search across all providers
    suspend fun smartSearch(query: String): Result<List<Media>>

    // Share links
    suspend fun createShareLink(type: ProviderType, assetIds: List<String>, expiresAt: Long? = null): Result<String>

    // Sync
    suspend fun uploadAsset(type: ProviderType, localMedia: Media, targetPath: String? = null): Result<CloudMediaEntity>
    suspend fun downloadAsset(type: ProviderType, remoteId: String): Result<android.net.Uri>
    suspend fun getChangedSince(type: ProviderType, timestamp: Long): Result<List<CloudMediaEntity>>

    // Search
    suspend fun search(query: String): Result<List<CloudMediaEntity>>

    // Archive
    suspend fun toggleArchive(type: ProviderType, remoteId: String, archived: Boolean): Result<Unit>
    fun getRemoteArchived(type: ProviderType): Flow<Resource<List<CloudMediaEntity>>>
    suspend fun getCachedArchivedAsync(): List<CloudMediaEntity>

    // Shared links management
    fun getSharedLinks(type: ProviderType): Flow<Resource<List<SharedLinkInfo>>>
    suspend fun deleteSharedLink(type: ProviderType, linkId: String): Result<Unit>
    suspend fun updateSharedLink(type: ProviderType, linkId: String, updates: Map<String, Any>): Result<Unit>

    // People editing
    suspend fun updatePersonName(type: ProviderType, personId: String, name: String): Result<Unit>
    suspend fun updatePersonBirthDate(type: ProviderType, personId: String, birthDate: String): Result<Unit>

    // Trash bulk operations
    suspend fun emptyTrash(type: ProviderType): Result<Unit>
    suspend fun restoreAllTrash(type: ProviderType): Result<Unit>

    // Memories
    fun getMemories(type: ProviderType): Flow<Resource<List<MemoryInfo>>>

    // Cache
    fun getCachedMedia(): Flow<List<CloudMediaEntity>>
    suspend fun getCachedMediaAsync(): List<CloudMediaEntity>
    fun getCachedFavorites(): Flow<List<CloudMediaEntity>>
    suspend fun getCachedFavoritesAsync(): List<CloudMediaEntity>
    fun getCachedTrashed(): Flow<List<CloudMediaEntity>>
    suspend fun getCachedTrashedAsync(): List<CloudMediaEntity>
    fun getCachedMediaByProvider(type: ProviderType): Flow<List<CloudMediaEntity>>
    suspend fun clearCache(type: ProviderType)
    suspend fun clearAllCache()
}
