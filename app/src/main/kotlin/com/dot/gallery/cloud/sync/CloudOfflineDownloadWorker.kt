/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudOfflinePinDao
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import com.dot.gallery.cloud.offline.CloudMediaCache
import com.dot.gallery.feature_node.presentation.util.printDebug
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import okhttp3.Request
import java.io.IOException

internal fun isRetryableOfflineHttpStatus(statusCode: Int): Boolean =
    statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode >= 500

/**
 * Downloads every cached asset of each "Available offline" account into the pinned cache tier
 * ([CloudMediaCache] pinned dir), so the grid (THUMBNAIL) and viewer (PREVIEW) render with no
 * network. Originals are intentionally NOT pinned in v1 to bound storage/bandwidth.
 *
 * Reports progress as `{done, total, configId}` via [setProgress].
 */
@HiltWorker
class CloudOfflineDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val registry: ProviderRegistry,
    private val pinDao: CloudOfflinePinDao,
    private val cloudMediaDao: CloudMediaDao,
    private val cache: CloudMediaCache
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pins = pinDao.getAllAsync()
        if (pins.isEmpty()) return Result.success()

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
        setProgress(workDataOf(KEY_DONE to 0, KEY_TOTAL to total, KEY_FAILED to 0))
        if (total == 0) return Result.success(workDataOf(KEY_DONE to 0, KEY_TOTAL to 0))

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
                setProgress(
                    workDataOf(
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
                setProgress(
                    workDataOf(
                        KEY_DONE to done,
                        KEY_TOTAL to total,
                        KEY_FAILED to failed,
                        KEY_CONFIG_ID to target.configId
                    )
                )
            }
        }

        val output = workDataOf(
            KEY_DONE to done,
            KEY_TOTAL to total,
            KEY_FAILED to failed,
            KEY_ERROR to (lastError ?: "")
        )
        printDebug("CloudOfflineDownloadWorker: pinned $done/$total variants; $failed failed")
        return when {
            failed == 0 -> Result.success(output)
            hasRetryableFailure && runAttemptCount < MAX_RETRY_ATTEMPTS -> Result.retry()
            else -> Result.failure(output)
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

    private fun retryOrFail(done: Int, total: Int, failed: Int, message: String): Result {
        if (runAttemptCount < MAX_RETRY_ATTEMPTS) return Result.retry()
        return Result.failure(
            workDataOf(
                KEY_DONE to done,
                KEY_TOTAL to total,
                KEY_FAILED to failed,
                KEY_ERROR to message
            )
        )
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
        private const val MAX_RETRY_ATTEMPTS = 3

        private val SIZES = listOf(
            Variant("thumbnail", ThumbnailSize.THUMBNAIL),
            Variant("preview", ThumbnailSize.PREVIEW)
        )

        fun triggerNow(workManager: WorkManager, wifiOnly: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<CloudOfflineDownloadWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
