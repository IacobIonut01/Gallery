/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.photoprism.data.api.PhotoPrismApiService
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismPhotoDto
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.core.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * PhotoPrism recoverable delete. App trash maps to batch archive/restore
 * (`deleted_at`); the archive UI maps to the private flag. Permanent delete and
 * empty-trash stay unsupported.
 */
internal class PhotoPrismTrashClient(
    private val cloudMediaDao: CloudMediaDao,
    private val api: () -> PhotoPrismApiService,
    private val configId: () -> Long,
    private val captureTokens: (okhttp3.Headers) -> Unit,
    private val mapPhotos: (List<PhotoPrismPhotoDto>) -> List<CloudMediaEntity>
) {
    fun remoteTrashed(): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val response = api().getPhotos(
                count = 1000,
                offset = 0,
                archived = true
            )
            if (response.isSuccessful) {
                captureTokens(response.headers())
                // PhotoPrism soft-delete uses deleted_at; list via archived=true and force trashed.
                val entities = mapPhotos(response.body().orEmpty()).map { it.copy(trashed = true) }
                cloudMediaDao.insertAll(entities)
                emit(Resource.Success(entities))
            } else {
                emit(Resource.Error("Failed to fetch trashed: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    fun remoteArchived(): Flow<Resource<List<CloudMediaEntity>>> = flow {
        try {
            val response = api().getPhotos(
                count = 1000,
                offset = 0,
                isPrivate = true
            )
            if (response.isSuccessful) {
                captureTokens(response.headers())
                // Private flag maps to archived in toCloudMediaEntity.
                emit(Resource.Success(mapPhotos(response.body().orEmpty())))
            } else {
                emit(Resource.Error("Failed to fetch archived: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    suspend fun toggleArchive(remoteId: String, archived: Boolean): Result<Unit> {
        return try {
            // PhotoPrism batch/private toggles the flag; skip when already at desired state.
            val accountId = configId()
            val current = cloudMediaDao.getByRemoteId(remoteId, ProviderType.PHOTOPRISM, accountId)
            if (current != null && current.archived == archived) {
                return Result.success(Unit)
            }
            val response = api().batchPrivatePhotos(mapOf("photos" to listOf(remoteId)))
            if (response.isSuccessful) {
                cloudMediaDao.updateArchived(remoteId, ProviderType.PHOTOPRISM, accountId, archived)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to toggle private: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trashAsset(remoteId: String): Result<Unit> {
        return try {
            val response = api().batchArchivePhotos(mapOf("photos" to listOf(remoteId)))
            if (response.isSuccessful) {
                cloudMediaDao.updateTrashed(remoteId, ProviderType.PHOTOPRISM, configId(), true)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to trash asset: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreAsset(remoteId: String): Result<Unit> {
        return try {
            val response = api().batchRestorePhotos(mapOf("photos" to listOf(remoteId)))
            if (response.isSuccessful) {
                cloudMediaDao.updateTrashed(remoteId, ProviderType.PHOTOPRISM, configId(), false)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to restore asset: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
