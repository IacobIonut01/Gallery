/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import android.content.Context
import com.dot.gallery.core.work.WorkComponentHolder
import com.dot.gallery.core.work.GalleryWorker
import com.dot.gallery.core.work.GalleryWorkerAdapter
import com.dot.gallery.core.work.WorkInput
import com.dot.gallery.core.work.WorkSignals
import com.dot.gallery.core.work.workData
import dev.brewkits.kmpworkmanager.annotations.Worker
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.Constraints
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.data.dao.CloudOfflinePinDao
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import okhttp3.Request
import java.io.IOException
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.offline.CloudMediaCache
import com.dot.gallery.core.logging.printDebug
import me.tatarka.inject.annotations.Inject


internal fun isRetryableOfflineHttpStatus(statusCode: Int): Boolean =
    statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode >= 500

/**
 * Downloads every cached asset of each "Available offline" account into the pinned cache tier
 * ([CloudMediaCache] pinned dir), so the grid (THUMBNAIL) and viewer (PREVIEW) render with no
 * network. Originals are intentionally NOT pinned in v1 to bound storage/bandwidth.
 *
 * Reports progress as `{done, total, configId}` via [setProgress].
 */
class CloudOfflineDownloadWorker @Inject constructor(
    private val registry: ProviderRegistry,
    private val pinDao: CloudOfflinePinDao,
    private val cloudMediaDao: CloudMediaDao,
    private val cache: CloudMediaCache
) : GalleryWorker {

    override suspend fun doWork(input: WorkInput, env: WorkerEnvironment): WorkerResult {
        val pins = pinDao.getAllAsync()
        if (pins.isEmpty()) return WorkerResult.Success()

        val client = CloudFetcherRegistryHolder.okHttpClient ?: return retryOrFail(
            done = 0,
            total = 0,
            failed = 1,
            message = "Cloud HTTP client is not ready"
        )

        // Include accounts whose provider is temporarily unavailable in the work set. Silently
        // dropping them would make the request succeed while none of their previews were saved.
        val targets = pins.map { pin ->
            DownloadTarget(
                configId = pin.serverConfigId,
                provider = registry.getByConfigId(pin.serverConfigId) as? RemoteMediaProvider,
                assets = cloudMediaDao.getByServerConfig(pin.serverConfigId).first()
            )
        }
        val total = targets.sumOf { it.assets.size } * SIZES.size
        reportProgress(workData(KEY_DONE to 0, KEY_TOTAL to total, KEY_FAILED to 0))
        if (total == 0) return WorkerResult.Success(data = workData(KEY_DONE to 0, KEY_TOTAL to 0))

        var done = 0
        var failed = 0
        var hasRetryableFailure = false
        var lastError: String? = null
        for (target in targets) {
            val provider = target.provider
            if (provider == null) {
                val missing = target.assets.size * SIZES.size
                failed += missing
                hasRetryableFailure = true
                lastError = "Cloud account ${target.configId} is not available"
                reportProgress(
                    workData(
                        KEY_DONE to done,
                        KEY_TOTAL to total,
                        KEY_FAILED to failed,
                        KEY_CONFIG_ID to target.configId
                    )
                )
                continue
            }
            val authHeaders = provider.getAuthHeaders()
            for (asset in target.assets) {
                currentCoroutineContext().ensureActive()
                for (size in SIZES) {
                    val key = cache.keyFor(asset.providerType, asset.serverConfigId, asset.remoteId, size.label)
                    val outcome = if (cache.isPinned(key)) {
                        DownloadOutcome.Success
                    } else {
                        downloadInto(provider, asset, size, key, authHeaders, client)
                    }
                    when (outcome) {
                        DownloadOutcome.Success -> done++
                        is DownloadOutcome.Failure -> {
                            failed++
                            hasRetryableFailure = hasRetryableFailure || outcome.retryable
                            lastError = outcome.message
                            printDebug(
                                "CloudOfflineDownloadWorker: failed ${asset.remoteId} " +
                                    "${size.label}: ${outcome.message}"
                            )
                        }
                    }
                }
                reportProgress(
                    workData(
                        KEY_DONE to done,
                        KEY_TOTAL to total,
                        KEY_FAILED to failed,
                        KEY_CONFIG_ID to target.configId
                    )
                )
            }
        }

        val output = workData(
            KEY_DONE to done,
            KEY_TOTAL to total,
            KEY_FAILED to failed,
            KEY_ERROR to (lastError ?: "")
        )
        printDebug("CloudOfflineDownloadWorker: pinned $done/$total variants; $failed failed")
        return when {
            failed == 0 -> WorkerResult.Success(data = output)
            hasRetryableFailure -> WorkerResult.Retry(
                reason = lastError ?: "offline downloads failed",
                attemptCap = MAX_RETRY_ATTEMPTS
            )
            else -> {
                WorkSignals.recordOutput(WORK_NAME, output)
                WorkerResult.Failure(lastError ?: "offline downloads failed")
            }
        }
    }

    private suspend fun downloadInto(
        provider: RemoteMediaProvider,
        asset: CloudMediaEntity,
        size: Variant,
        key: String,
        authHeaders: Map<String, String>,
        client: okhttp3.OkHttpClient
    ): DownloadOutcome = try {
        val url = provider.getThumbnailUrl(asset.remoteId, size.thumb, asset.fileId)
        // No server preview URL (e.g. a video on a path-based store like WebDAV that has
        // no preview endpoint). Decode a poster frame locally from the original stream.
        if (!url.startsWith("http", ignoreCase = true)) {
            val frame = provider.getVideoThumbnailBytes(asset.remoteId, size.thumb)
            if (frame == null || frame.isEmpty()) {
                DownloadOutcome.Failure("No offline preview is available", retryable = false)
            } else if (cache.storePinned(key, frame, "image/jpeg")) {
                DownloadOutcome.Success
            } else {
                DownloadOutcome.Failure("Unable to save the offline preview", retryable = false)
            }
        } else {
            val builder = Request.Builder().url(url)
            authHeaders.forEach { (k, v) -> builder.addHeader(k, v) }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use DownloadOutcome.Failure(
                        message = "HTTP ${response.code}: ${response.message}",
                        retryable = isRetryableOfflineHttpStatus(response.code)
                    )
                }
                val responseBody = response.body
                val contentType = responseBody.contentType()?.toString()
                val bytes = responseBody.bytes()
                when {
                    bytes.isEmpty() -> DownloadOutcome.Failure("Empty preview response", retryable = true)
                    cache.storePinned(key, bytes, contentType) -> DownloadOutcome.Success
                    else -> DownloadOutcome.Failure("Unable to save the offline preview", retryable = false)
                }
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        DownloadOutcome.Failure(
            message = error.message ?: error::class.java.simpleName,
            retryable = error is IOException ||
                (error !is IllegalArgumentException && error !is UnsupportedOperationException)
        )
    }

    private fun reportProgress(data: kotlinx.serialization.json.JsonObject) {
        WorkSignals.reportProgress(WORK_NAME, data)
    }

    private fun retryOrFail(done: Int, total: Int, failed: Int, message: String): WorkerResult {
        val output = workData(
            KEY_DONE to done,
            KEY_TOTAL to total,
            KEY_FAILED to failed,
            KEY_ERROR to message
        )
        WorkSignals.recordOutput(WORK_NAME, output)
        // Retryable path (e.g. HTTP client not ready yet) — capped like the old
        // runAttemptCount < MAX_RETRY_ATTEMPTS gate.
        return WorkerResult.Retry(reason = message, attemptCap = MAX_RETRY_ATTEMPTS)
    }

    private data class DownloadTarget(
        val configId: Long,
        val provider: RemoteMediaProvider?,
        val assets: List<CloudMediaEntity>
    )

    private sealed interface DownloadOutcome {
        data object Success : DownloadOutcome
        data class Failure(val message: String, val retryable: Boolean) : DownloadOutcome
    }

    private data class Variant(val label: String, val thumb: ThumbnailSize)

    companion object {
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_FAILED = "failed"
        const val KEY_CONFIG_ID = "configId"
        const val KEY_ERROR = "error"
        const val WORK_NAME = "cloud_offline_download"
        const val WORKER_NAME = "CloudOfflineDownloadWorker"
        const val PROGRESS_WORK_NAME = WORK_NAME
        private const val MAX_RETRY_ATTEMPTS = 3

        private val SIZES = listOf(
            Variant("thumbnail", ThumbnailSize.THUMBNAIL),
            Variant("preview", ThumbnailSize.PREVIEW)
        )

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

@Worker(name = CloudOfflineDownloadWorker.WORKER_NAME)
class CloudOfflineDownloadWorkerAdapter :
    GalleryWorkerAdapter(CloudOfflineDownloadWorker.WORKER_NAME) {
    override suspend fun run(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        WorkComponentHolder.component.cloudOfflineDownloadWorker.doWork(input, env)
}
