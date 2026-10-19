/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import android.content.Context
import android.net.Uri
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.cloud.photoprism.data.api.PhotoPrismApiService
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismImportOptionsDto
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismUploadOptionsDto
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.getUri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

/**
 * PhotoPrism backup upload. Stages multipart files on
 * `POST /api/v1/users/{uid}/upload/{token}`, then [finalizeBatch] runs the web UI
 * process step and `POST /api/v1/import/` for `/upload/{token}`.
 */
internal class PhotoPrismUploadTarget(
    private val context: Context,
    private val cloudMediaDao: CloudMediaDao,
    private val api: () -> PhotoPrismApiService,
    private val configId: () -> Long
) {
    /** PhotoPrism user UID required by the upload API path. */
    @Volatile
    var userUid: String? = null

    /**
     * Client-side token that groups a backup-worker batch of multipart uploads.
     * PhotoPrism stages files under users/{uid}/upload/{token}.
     */
    @Volatile
    private var uploadBatchToken: String? = null

    private val pendingUploadCount = AtomicInteger(0)

    fun clear() {
        userUid = null
        uploadBatchToken = null
        pendingUploadCount.set(0)
    }

    /**
     * Relative import path for the current upload batch (`/upload/{token}`).
     * PhotoPrism remaps this with the session ref so staged user uploads are indexed.
     */
    fun currentUploadImportPath(): String? =
        uploadBatchToken?.let { "/upload/$it" }

    suspend fun uploadAsset(localMedia: Media, contentHash: String?): Result<CloudMediaEntity> {
        return try {
            val uid = requireUserUid()
            val token = ensureUploadBatchToken()
            val mediaUri = localMedia.getUri()
            val mimeType = localMedia.mimeType
            val fileName = localMedia.label
            val requestBody = ContentResolverRequestBody(
                context = context,
                uri = mediaUri,
                mediaType = mimeType.toMediaTypeOrNull(),
                length = localMedia.size.takeIf { it > 0L } ?: -1L
            )
            // PhotoPrism expects the multipart field name "files".
            val filePart = MultipartBody.Part.createFormData("files", fileName, requestBody)
            val response = api().uploadUserFiles(uid, token, filePart)
            if (response.isSuccessful) {
                pendingUploadCount.incrementAndGet()
                val importPath = currentUploadImportPath().orEmpty()
                val entity = CloudMediaEntity(
                    remoteId = "upload:$token:$fileName",
                    providerType = ProviderType.PHOTOPRISM,
                    serverConfigId = configId(),
                    label = fileName,
                    path = importPath,
                    relativePath = importPath,
                    mimeType = mimeType,
                    timestamp = System.currentTimeMillis(),
                    size = localMedia.size,
                    syncState = SyncState.SYNCED,
                    localCopyPath = mediaUri.toString(),
                    contentHash = contentHash
                )
                cloudMediaDao.insert(entity)
                Result.success(entity)
            } else {
                Result.failure(
                    Exception("PhotoPrism upload failed: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bulkUploadCheck(hashes: List<String>): Result<Map<String, Boolean>> {
        // No server-side bulk checksum API; best-effort against the local contentHash cache.
        return try {
            val accountId = configId()
            val result = hashes.mapIndexed { index, hash ->
                val exists = if (hash.isBlank() || accountId == 0L) {
                    false
                } else {
                    val cached = cloudMediaDao.getByContentHash(hash, accountId)
                    cached != null && cached.providerType == ProviderType.PHOTOPRISM
                }
                index.toString() to exists
            }.toMap()
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Best-effort: treat as present when Room already has a PhotoPrism row for this
     * account with the same file name (and matching size when known).
     */
    suspend fun remoteExists(localMedia: Media): Boolean {
        return try {
            val accountId = configId().takeIf { it != 0L } ?: return false
            val label = localMedia.label
            val localSize = runCatching {
                context.contentResolver.openAssetFileDescriptor(localMedia.getUri(), "r")?.use { it.length }
            }.getOrNull()
            cloudMediaDao.getAllCachedAsync().any { entity ->
                entity.providerType == ProviderType.PHOTOPRISM &&
                    entity.serverConfigId == accountId &&
                    entity.label.equals(label, ignoreCase = true) &&
                    (localSize == null || localSize <= 0L || entity.size <= 0L || entity.size == localSize)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    /**
     * After a backup batch: process staged user uploads (PUT), then trigger
     * `POST /api/v1/import/` for `/upload/{token}` so PhotoPrism indexes the folder.
     */
    suspend fun finalizeBatch(): Result<Unit> {
        val token = uploadBatchToken
        val uid = userUid
        if (token.isNullOrBlank() || uid.isNullOrBlank() || pendingUploadCount.get() <= 0) {
            uploadBatchToken = null
            pendingUploadCount.set(0)
            return Result.success(Unit)
        }
        val importPath = "/upload/$token"
        return try {
            // Native finalize used by PhotoPrism's own web UI (moves staged files → originals).
            val process = api().processUserUpload(uid, token, PhotoPrismUploadOptionsDto())
            if (!process.isSuccessful) {
                printDebug(
                    "PhotoPrismProvider: process upload failed ${process.code()} ${process.message()}"
                )
            }

            // Explicit import trigger for the staged upload folder path.
            val importResp = api().startImport(
                PhotoPrismImportOptionsDto(path = importPath, move = true)
            )
            if (importResp.isSuccessful) {
                printDebug("PhotoPrismProvider: import started for $importPath")
                Result.success(Unit)
            } else if (process.isSuccessful) {
                // PUT already indexed; import may 403 for limited roles — still OK.
                printDebug(
                    "PhotoPrismProvider: import returned ${importResp.code()} after successful process"
                )
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(
                        "PhotoPrism import failed: ${importResp.code()} ${importResp.message()}"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            uploadBatchToken = null
            pendingUploadCount.set(0)
        }
    }

    private fun ensureUploadBatchToken(): String {
        uploadBatchToken?.let { return it }
        synchronized(this) {
            uploadBatchToken?.let { return it }
            val token = UUID.randomUUID().toString().replace("-", "").take(8)
            uploadBatchToken = token
            return token
        }
    }

    private fun requireUserUid(): String =
        userUid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "PhotoPrism user UID unknown — re-authenticate (session must include user.UID)"
            )
}

private class ContentResolverRequestBody(
    private val context: Context,
    private val uri: Uri,
    private val mediaType: MediaType?,
    private val length: Long
) : RequestBody() {
    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long = length

    override fun writeTo(sink: BufferedSink) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open media file")
        input.source().use { source -> sink.writeAll(source) }
    }
}
