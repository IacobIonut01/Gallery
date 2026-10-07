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
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.RemoteAlbumCopyResult
import com.dot.gallery.cloud.core.capabilities.RemoteAlbumCopyState
import com.dot.gallery.cloud.core.capabilities.RemoteAlbumWriteProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.RemoteNameConflictPolicy
import com.dot.gallery.cloud.core.capabilities.SyncDelta
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.data.repository.getRemoteAlbumMediaForAccount
import com.dot.gallery.cloud.util.resolveCloudDownloadProvider
import com.dot.gallery.core.Resource
import com.dot.gallery.domain.model.Media
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.domain.model.EncryptedMedia
import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.repository.copyRemoteAlbumForAccount
import com.dot.gallery.cloud.image.awaitInitializedRemoteProvider
import com.dot.gallery.domain.uri.path
import com.dot.gallery.domain.uri.query
import com.dot.gallery.cloud.data.repository.resolveProviderAccount


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
    fun explicitTransferDownloadNeverFallsBackToAnotherSameTypeAccount() {
        val registry = ProviderRegistry()
        val first = AlbumProvider(configId = 101L)
        val second = AlbumProvider(configId = 202L)
        registry._providers[101L] = first
        registry._providers[202L] = second

        assertTrue(
            resolveCloudDownloadProvider(
                registry,
                ProviderType.IMMICH,
                202L,
                requireExactAccount = true
            ) === second
        )
        assertEquals(
            null,
            resolveCloudDownloadProvider(
                registry,
                ProviderType.IMMICH,
                303L,
                requireExactAccount = true
            )
        )
        assertTrue(
            resolveCloudDownloadProvider(
                registry,
                ProviderType.IMMICH,
                303L,
                requireExactAccount = false
            ) === first
        )
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

    @Test
    fun sameProviderAccountsCopyIntoRequestedAccountAndAlbum() = runBlocking {
        val registry = ProviderRegistry()
        val first = AlbumProvider(configId = 101L)
        val second = AlbumProvider(configId = 202L)
        registry._providers[101L] = first
        registry._providers[202L] = second
        val media = EncryptedMedia(
            id = 1L,
            label = "photo.jpg",
            bytes = byteArrayOf(1),
            path = "",
            relativePath = "Pictures",
            albumID = 1L,
            albumLabel = "Pictures",
            timestamp = 1L,
            fullDate = "",
            mimeType = "image/jpeg",
            favorite = 0,
            trashed = 0,
            size = 1L
        )

        val result = copyRemoteAlbumForAccount(
            registry = registry,
            type = ProviderType.IMMICH,
            configId = 202L,
            remoteAlbumId = "shared-album",
            localMedia = media,
            conflictPolicy = RemoteNameConflictPolicy.KEEP_BOTH,
            checksum = "hash",
            continuationRemoteId = null
        )

        assertEquals(RemoteAlbumCopyState.COPIED, result.state)
        assertEquals(emptyList<String>(), first.copyRequests)
        assertEquals(listOf("shared-album"), second.copyRequests)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cloudImageProviderWaitsUntilAccountInitializationCompletes() = runTest {
        val registry = ProviderRegistry()
        val pending = async {
            awaitInitializedRemoteProvider(
                registry,
                ProviderType.IMMICH,
                202L,
                timeoutMillis = 5_000L
            )
        }
        runCurrent()
        assertFalse(pending.isCompleted)
        val provider = AlbumProvider(configId = 202L)
        registry._providers[202L] = provider
        registry.updateConnectionState(202L, ConnectionState.AUTHENTICATING)
        runCurrent()
        assertFalse(pending.isCompleted)

        registry.updateConnectionState(202L, ConnectionState.CONNECTED)
        runCurrent()

        assertSame(provider, pending.await())
    }

    @Test
    fun cloudImageProviderCanUseConfiguredAccountAfterAuthenticationFailure() = runTest {
        val registry = ProviderRegistry()
        val provider = AlbumProvider(configId = 202L)
        registry._providers[202L] = provider
        registry.updateConnectionState(202L, ConnectionState.ERROR)

        assertSame(
            provider,
            awaitInitializedRemoteProvider(
                registry,
                ProviderType.IMMICH,
                202L,
                timeoutMillis = 5_000L
            )
        )
    }

    private class AlbumProvider(private val configId: Long) :
        RemoteMediaProvider,
        RemoteAlbumWriteProvider {
        val requestedAlbums = mutableListOf<String>()
        val archiveRequests = mutableListOf<String>()
        val copyRequests = mutableListOf<String>()

        override val providerType = ProviderType.IMMICH
        override val displayName = "Immich $configId"
        override val isAvailable = true
        override val capabilities = setOf(
            ProviderCapability.REMOTE_ALBUMS,
            ProviderCapability.SYNC,
            ProviderCapability.ALBUM_WRITE
        )
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
        override suspend fun uploadAsset(localMedia: Media, targetPath: String?) =
            Result.success(
                CloudMediaEntity(
                    remoteId = "uploaded-$configId",
                    providerType = providerType,
                    serverConfigId = configId
                )
            )
        override suspend fun downloadAsset(remoteId: String) =
            Result.failure<android.net.Uri>(UnsupportedOperationException())
        override suspend fun getSyncDelta(timestamp: Long, reconcileIndex: Boolean) =
            Result.success(SyncDelta(items = emptyList()))
        override suspend fun bulkUploadCheck(hashes: List<String>) =
            Result.success(emptyMap<String, Boolean>())
        override suspend fun copyToAlbum(
            media: Media,
            remoteAlbumId: String,
            conflictPolicy: RemoteNameConflictPolicy,
            checksum: String?,
            continuationRemoteId: String?
        ): RemoteAlbumCopyResult {
            copyRequests += remoteAlbumId
            return RemoteAlbumCopyResult(
                state = RemoteAlbumCopyState.COPIED,
                remoteId = "uploaded-$configId"
            )
        }

        private fun emptyMedia(): Flow<Resource<List<CloudMediaEntity>>> =
            flowOf(Resource.Success(emptyList()))
    }
}
