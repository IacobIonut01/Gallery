/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import com.dot.gallery.cloud.offline.CloudMediaCache
import com.dot.gallery.cloud.offline.OfflineModeManager
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.core.work.GalleryWorker
import com.dot.gallery.core.work.GalleryWorkerAdapter
import com.dot.gallery.core.work.WorkComponentHolder
import com.dot.gallery.core.work.WorkInput
import com.dot.gallery.core.work.WorkSignals
import com.dot.gallery.core.work.workData
import com.dot.gallery.data.dao.CloudMediaDao
import dev.brewkits.kmpworkmanager.annotations.Worker
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.Constraints
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import kotlinx.coroutines.CancellationException
import me.tatarka.inject.annotations.Inject
import okhttp3.Request

/**
 * Pins full-resolution originals for every cloud favorite when the user enables
 * [OfflineModeManager.downloadFavoritesFullRes]. Complements [CloudOfflineDownloadWorker]
 * (thumb + preview for whole accounts) with original-sized favorites only.
 */
class CloudFavoriteOfflineWorker @Inject constructor(
    private val registry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao,
    private val cache: CloudMediaCache,
    private val offlineModeManager: OfflineModeManager
) : GalleryWorker {

    override suspend fun doWork(input: WorkInput, env: WorkerEnvironment): WorkerResult {
        if (!offlineModeManager.downloadFavoritesFullResNow) return WorkerResult.Success()

        val favorites = cloudMediaDao.getFavoritesAsync()
        if (favorites.isEmpty()) return WorkerResult.Success()

        val client = CloudFetcherRegistryHolder.okHttpClient ?: return retryOrFail(
            done = 0,
            total = favorites.size,
            message = "Cloud HTTP client is not ready"
        )

        val total = favorites.size
        var done = 0
        var failures = 0
        reportProgress(done, total)

        for (asset in favorites) {
            val key = cache.keyFor(asset.providerType, asset.serverConfigId, asset.remoteId, SIZE_ORIGINAL)
            if (!cache.isPinned(key)) {
                val provider = registry.getByConfigId(asset.serverConfigId) as? RemoteMediaProvider
                    ?: registry.get(asset.providerType) as? RemoteMediaProvider
                if (provider == null) {
                    failures++
                } else {
                    // Seed PhotoPrism's in-memory hash map from Room's fileId when needed.
                    if (asset.fileId.isNotBlank()) {
                        provider.getThumbnailUrl(asset.remoteId, ThumbnailSize.THUMBNAIL, asset.fileId)
                    }
                    val url = provider.getOriginalUrl(asset.remoteId).ifBlank { asset.originalUrl }
                    if (!downloadOriginal(url, key, provider.getAuthHeaders(), client)) failures++
                }
            }
            done++
            reportProgress(done, total)
        }

        printDebug("CloudFavoriteOfflineWorker: pinned originals $done/$total (failures=$failures)")
        val output = workData(KEY_DONE to done, KEY_TOTAL to total)
        return if (failures > 0 && done == failures) {
            WorkerResult.Retry(reason = "favorite originals failed", attemptCap = MAX_RETRY_ATTEMPTS)
        } else {
            WorkerResult.Success(data = output)
        }
    }

    private fun downloadOriginal(
        url: String,
        key: String,
        authHeaders: Map<String, String>,
        client: okhttp3.OkHttpClient
    ): Boolean = runCatching {
        if (!url.startsWith("http", ignoreCase = true)) return false
        val builder = Request.Builder().url(url)
        authHeaders.forEach { (header, value) -> builder.addHeader(header, value) }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return false
            val body = response.body
            val bytes = body.bytes()
            if (bytes.isEmpty()) return false
            cache.storePinned(key, bytes, body.contentType()?.toString())
        }
    }.onFailure { error ->
        if (error is CancellationException) throw error
        printDebug("CloudFavoriteOfflineWorker: failed $url: ${error.message}")
    }.getOrDefault(false)

    private fun reportProgress(done: Int, total: Int) {
        WorkSignals.reportProgress(WORK_NAME, workData(KEY_DONE to done, KEY_TOTAL to total))
    }

    private fun retryOrFail(done: Int, total: Int, message: String): WorkerResult {
        val output = workData(KEY_DONE to done, KEY_TOTAL to total, KEY_ERROR to message)
        WorkSignals.recordOutput(WORK_NAME, output)
        return WorkerResult.Retry(reason = message, attemptCap = MAX_RETRY_ATTEMPTS)
    }

    companion object {
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
        const val WORK_NAME = "cloud_favorite_offline_download"
        const val WORKER_NAME = "CloudFavoriteOfflineWorker"
        const val SIZE_ORIGINAL = "original"
        private const val MAX_RETRY_ATTEMPTS = 3

        suspend fun triggerNow(scheduler: BackgroundTaskScheduler, wifiOnly: Boolean) {
            scheduler.enqueue(
                id = WORK_NAME,
                trigger = TaskTrigger.OneTime(),
                workerClassName = WORKER_NAME,
                constraints = Constraints(
                    requiresNetwork = true,
                    requiresUnmeteredNetwork = wifiOnly,
                    isHeavyTask = true,
                    maxRetries = MAX_RETRY_ATTEMPTS - 1,
                ),
                policy = ExistingPolicy.REPLACE,
            )
        }
    }
}

@Worker(name = CloudFavoriteOfflineWorker.WORKER_NAME)
class CloudFavoriteOfflineWorkerAdapter :
    GalleryWorkerAdapter(CloudFavoriteOfflineWorker.WORK_NAME) {
    override suspend fun run(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        WorkComponentHolder.component.cloudFavoriteOfflineWorker.doWork(input, env)
}
