/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudAuthToken
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.core.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RemoteMediaProvider : MediaCapabilityProvider {
    val connectionState: StateFlow<ConnectionState>

    suspend fun testConnection(config: CloudServerConfig): Result<CloudServerInfo>
    suspend fun authenticate(config: CloudServerConfig): Result<CloudAuthToken>

    fun getRemoteAssets(page: Int, pageSize: Int): Flow<Resource<List<CloudMediaEntity>>>
    fun getRemoteAlbums(): Flow<Resource<List<CloudAlbum>>>
    fun getRemoteAlbumMedia(albumId: String): Flow<Resource<List<CloudMediaEntity>>>
    fun getRemoteFavorites(): Flow<Resource<List<CloudMediaEntity>>>
    fun getRemoteTrashed(): Flow<Resource<List<CloudMediaEntity>>>

    suspend fun toggleFavorite(remoteId: String, favorite: Boolean): Result<Unit>
    suspend fun toggleArchive(remoteId: String, archived: Boolean): Result<Unit>
    suspend fun trashAsset(remoteId: String): Result<Unit>
    suspend fun restoreAsset(remoteId: String): Result<Unit>
    suspend fun deleteAsset(remoteId: String): Result<Unit>
    suspend fun emptyTrash(): Result<Unit>
    suspend fun restoreAllTrash(): Result<Unit>
    suspend fun createAlbum(name: String): Result<CloudAlbum>
    suspend fun addToAlbum(albumId: String, assetIds: List<String>): Result<Unit>
    suspend fun search(query: String): Result<List<CloudMediaEntity>>

    fun getRemoteArchived(): Flow<Resource<List<CloudMediaEntity>>>

    suspend fun getStorageInfo(): Result<CloudStorageInfo>
    suspend fun getServerVersion(): Result<String> = Result.failure(UnsupportedOperationException())

    fun getThumbnailUrl(remoteId: String, size: ThumbnailSize = ThumbnailSize.PREVIEW): String
    fun getOriginalUrl(remoteId: String): String
    fun getAuthHeaders(): Map<String, String>

    fun configure(config: CloudServerConfig)
}
