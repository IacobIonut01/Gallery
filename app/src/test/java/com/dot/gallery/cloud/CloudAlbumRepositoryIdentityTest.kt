/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudAuthToken
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.CloudServerInfo
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.data.repository.getRemoteAlbumMediaForAccount
import com.dot.gallery.cloud.data.repository.resolveProviderAccount
import com.dot.gallery.core.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudAlbumRepositoryIdentityTest {

    @Test
    fun sameProviderAccountsResolveEveryOperationThroughRequestedConfigId() = runBlocking {
        val registry = ProviderRegistry()
        val first = AlbumProvider(configId = 101L)
        val second = AlbumProvider(configId = 202L)
        registry._providers[101L] = first
        registry._providers[202L] = second

        val resolved = resolveProviderAccount<RemoteMediaProvider>(
            registry = registry,
            type = ProviderType.IMMICH,
            configId = 202L,
            capabilityName = "archive"
        ).getOrThrow()
        resolved.toggleArchive("same-remote-id", false)

        assertEquals(emptyList<String>(), first.archiveRequests)
        assertEquals(listOf("same-remote-id"), second.archiveRequests)
    }

    @Test
    fun sameProviderAccountsResolveAlbumThroughRequestedConfigId() = runBlocking {
        val registry = ProviderRegistry()
        val first = AlbumProvider(configId = 101L)
        val second = AlbumProvider(configId = 202L)
        registry._providers[101L] = first
        registry._providers[202L] = second

        val result = getRemoteAlbumMediaForAccount(
            registry = registry,
            type = ProviderType.IMMICH,
            configId = 202L,
            albumId = "shared-album"
        ).first()

        assertTrue(result is Resource.Success)
        assertEquals(202L, result.data?.single()?.serverConfigId)
        assertEquals(emptyList<String>(), first.requestedAlbums)
        assertEquals(listOf("shared-album"), second.requestedAlbums)
    }

    private class AlbumProvider(private val configId: Long) : RemoteMediaProvider {
        val requestedAlbums = mutableListOf<String>()
        val archiveRequests = mutableListOf<String>()

        override val providerType = ProviderType.IMMICH
        override val displayName = "Immich $configId"
        override val isAvailable = true
        override val capabilities = setOf(ProviderCapability.REMOTE_ALBUMS)
        override val connectionState = MutableStateFlow(ConnectionState.CONNECTED)

        override fun getRemoteAlbumMedia(albumId: String): Flow<Resource<List<CloudMediaEntity>>> {
            requestedAlbums += albumId
            return flowOf(
                Resource.Success(
                    listOf(
                        CloudMediaEntity(
                            remoteId = "asset",
                            providerType = providerType,
                            serverConfigId = configId,
                            label = "asset.jpg"
                        )
                    )
                )
            )
        }

        override suspend fun testConnection(config: CloudServerConfig) =
            Result.failure<CloudServerInfo>(UnsupportedOperationException())
        override suspend fun authenticate(config: CloudServerConfig) =
            Result.failure<CloudAuthToken>(UnsupportedOperationException())
        override fun getRemoteAssets(page: Int, pageSize: Int) = emptyMedia()
        override fun getRemoteAlbums(): Flow<Resource<List<CloudAlbum>>> =
            flowOf(Resource.Success(emptyList()))
        override fun getRemoteFavorites() = emptyMedia()
        override fun getRemoteTrashed() = emptyMedia()
        override suspend fun toggleFavorite(remoteId: String, favorite: Boolean) = Result.success(Unit)
        override suspend fun toggleArchive(remoteId: String, archived: Boolean): Result<Unit> {
            archiveRequests += remoteId
            return Result.success(Unit)
        }
        override suspend fun trashAsset(remoteId: String) = Result.success(Unit)
        override suspend fun restoreAsset(remoteId: String) = Result.success(Unit)
        override suspend fun deleteAsset(remoteId: String) = Result.success(Unit)
        override suspend fun emptyTrash() = Result.success(Unit)
        override suspend fun restoreAllTrash() = Result.success(Unit)
        override suspend fun createAlbum(name: String) =
            Result.failure<CloudAlbum>(UnsupportedOperationException())
        override suspend fun addToAlbum(albumId: String, assetIds: List<String>) = Result.success(Unit)
        override suspend fun search(query: String) = Result.success(emptyList<CloudMediaEntity>())
        override fun getRemoteArchived() = emptyMedia()
        override suspend fun getStorageInfo() =
            Result.failure<CloudStorageInfo>(UnsupportedOperationException())
        override fun getThumbnailUrl(remoteId: String, size: ThumbnailSize) = ""
        override fun getOriginalUrl(remoteId: String) = ""
        override fun getAuthHeaders() = emptyMap<String, String>()
        override fun configure(config: CloudServerConfig) = Unit

        private fun emptyMedia(): Flow<Resource<List<CloudMediaEntity>>> =
            flowOf(Resource.Success(emptyList()))
    }
}
