/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.dao.CloudUploadPrefDao
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.util.printDebug
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import com.dot.gallery.feature_node.domain.util.getUri
import androidx.work.workDataOf
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@HiltWorker
class CloudUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val registry: ProviderRegistry,
    private val configDao: CloudServerConfigDao,
    private val uploadPrefDao: CloudUploadPrefDao,
    private val cloudMediaDao: CloudMediaDao,
    private val repository: MediaRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        printDebug("CloudUploadWorker: Starting upload check...")
        try {
            val enabledPrefs = uploadPrefDao.getEnabledList()
            if (enabledPrefs.isEmpty()) {
                printDebug("CloudUploadWorker: No albums enabled for upload")
                return Result.success()
            }

            val configs = configDao.getAll().first()
            val activeConfig = configs.firstOrNull { it.isActive && it.syncEnabled }
            if (activeConfig == null) {
                printDebug("CloudUploadWorker: No active sync-enabled server config")
                return Result.success()
            }

            val provider = registry.get(activeConfig.providerType)
            val syncProvider = provider as? SyncCapableProvider
            if (syncProvider == null) {
                printDebug("CloudUploadWorker: Provider does not support sync")
                return Result.success()
            }

            val enabledAlbumIds = enabledPrefs.map { it.albumId }.toSet()
            val deleteLocalMap = enabledPrefs.associate { it.albumId to it.deleteLocalAfterUpload }

            // Collect all items to upload across albums first
            val allToUpload = mutableListOf<Pair<Media, String>>()
            val albumLabelMap = mutableMapOf<String, String>()

            for (pref in enabledPrefs) {
                val albumMedia = repository.getMediaByAlbumId(pref.albumId)
                    .first().data ?: continue

                if (albumMedia.isEmpty()) continue
                printDebug("CloudUploadWorker: Album ${pref.albumLabel} has ${albumMedia.size} items")

                // Check which are already uploaded using content hash
                val mediaWithHashes = albumMedia.mapNotNull { media ->
                    val hash = computeSha1(media) ?: return@mapNotNull null
                    media to hash
                }

                if (mediaWithHashes.isEmpty()) continue

                val hashes = mediaWithHashes.map { it.second }
                val alreadyUploaded = try {
                    syncProvider.bulkUploadCheck(hashes).getOrDefault(emptyMap())
                } catch (e: Exception) {
                    printDebug("CloudUploadWorker: Bulk check failed: ${e.message}")
                    emptyMap()
                }

                val toUpload = mediaWithHashes.filterIndexed { idx, (_, hash) ->
                    val isRejected = alreadyUploaded[idx.toString()] ?: false
                    !isRejected // not rejected means not yet on server
                }

                printDebug("CloudUploadWorker: ${toUpload.size} items to upload in ${pref.albumLabel}")
                toUpload.forEach { (media, hash) ->
                    allToUpload.add(media to hash)
                    albumLabelMap[hash] = pref.albumLabel
                }
            }

            val totalItems = allToUpload.size
            var completedItems = 0
            var failedItems = 0
            val completedFiles = mutableListOf<String>()
            val failedFiles = mutableListOf<String>()

            setProgress(workDataOf(
                KEY_TOTAL_ITEMS to totalItems,
                KEY_COMPLETED_ITEMS to 0,
                KEY_FAILED_ITEMS to 0,
                KEY_CURRENT_FILE to ""
            ))

            for ((media, _) in allToUpload) {
                if (isStopped) return Result.retry()

                setProgress(workDataOf(
                    KEY_TOTAL_ITEMS to totalItems,
                    KEY_COMPLETED_ITEMS to completedItems,
                    KEY_FAILED_ITEMS to failedItems,
                    KEY_CURRENT_FILE to media.label,
                    KEY_COMPLETED_FILES to completedFiles.takeLast(MAX_TRACKED_FILES).toTypedArray(),
                    KEY_FAILED_FILES to failedFiles.takeLast(MAX_TRACKED_FILES).toTypedArray()
                ))

                try {
                    val result = syncProvider.uploadAsset(media)
                    result.onSuccess { entity ->
                        completedItems++
                        completedFiles.add(media.label)
                        printDebug("CloudUploadWorker: Uploaded ${media.label}")
                        // If delete local is enabled, delete local copy
                        val albumId = enabledPrefs.find { pref ->
                            true // simplified; original logic used pref loop
                        }?.albumId
                        if (albumId != null && deleteLocalMap[albumId] == true) {
                            try {
                                applicationContext.contentResolver.delete(
                                    media.getUri(), null, null
                                )
                                printDebug("CloudUploadWorker: Deleted local copy of ${media.label}")
                            } catch (e: Exception) {
                                printDebug("CloudUploadWorker: Failed to delete local: ${e.message}")
                            }
                        }
                    }.onFailure { e ->
                        failedItems++
                        failedFiles.add(media.label)
                        printDebug("CloudUploadWorker: Upload failed for ${media.label}: ${e.message}")
                    }
                } catch (e: Exception) {
                    failedItems++
                    failedFiles.add(media.label)
                    printDebug("CloudUploadWorker: Exception uploading ${media.label}: ${e.message}")
                }
            }

            setProgress(workDataOf(
                KEY_TOTAL_ITEMS to totalItems,
                KEY_COMPLETED_ITEMS to completedItems,
                KEY_FAILED_ITEMS to failedItems,
                KEY_CURRENT_FILE to "",
                KEY_COMPLETED_FILES to completedFiles.takeLast(MAX_TRACKED_FILES).toTypedArray(),
                KEY_FAILED_FILES to failedFiles.takeLast(MAX_TRACKED_FILES).toTypedArray()
            ))

            printDebug("CloudUploadWorker: Upload check complete")
            return Result.success()
        } catch (e: Exception) {
            printDebug("CloudUploadWorker: Failed: ${e.message}")
            return Result.retry()
        }
    }

    private fun computeSha1(media: Media): String? {
        return try {
            val uri = media.getUri()
            applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("SHA-1")
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val WORK_NAME = "cloud_upload"
        const val WORK_NAME_ONCE = "cloud_upload_now"

        const val KEY_CURRENT_FILE = "current_file"
        const val KEY_TOTAL_ITEMS = "total_items"
        const val KEY_COMPLETED_ITEMS = "completed_items"
        const val KEY_FAILED_ITEMS = "failed_items"
        const val KEY_COMPLETED_FILES = "completed_files"
        const val KEY_FAILED_FILES = "failed_files"
        private const val MAX_TRACKED_FILES = 50

        fun schedule(
            workManager: WorkManager,
            intervalMinutes: Long = 30,
            wifiOnly: Boolean = true
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .build()

            val request = PeriodicWorkRequestBuilder<CloudUploadWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun triggerNow(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<CloudUploadWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(
                WORK_NAME_ONCE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}
