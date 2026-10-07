/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import android.content.Context
import android.net.ConnectivityManager
import com.dot.gallery.core.work.WorkComponentHolder
import com.dot.gallery.core.work.GalleryWorker
import com.dot.gallery.core.work.GalleryWorkerAdapter
import com.dot.gallery.core.work.WorkInput
import com.dot.gallery.core.work.workDataJson
import com.dot.gallery.core.work.workScheduler
import dev.brewkits.kmpworkmanager.annotations.Worker
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.BackoffPolicy
import dev.brewkits.kmpworkmanager.background.domain.Constraints
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.core.capabilities.SyncDelta
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.data.dao.CloudMediaLocalState
import com.dot.gallery.data.dao.CloudServerConfigDao
import com.dot.gallery.data.dao.SyncStateDao
import com.dot.gallery.data.entity.CloudServerConfigEntity
import com.dot.gallery.data.entity.SyncStateEntity
import com.dot.gallery.core.workers.enqueueCaptureTimeIndex
import com.dot.gallery.data.entity.SmartScanFeature
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import me.tatarka.inject.annotations.Inject
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.core.smart.SmartScanScheduler
import com.dot.gallery.core.logging.printDebug


class CloudSyncWorker @Inject constructor(
    private val context: Context,
    private val registry: ProviderRegistry,
    private val configDao: CloudServerConfigDao,
    private val syncStateDao: SyncStateDao,
    private val cloudMediaDao: CloudMediaDao,
    private val smartScanScheduler: SmartScanScheduler
) : GalleryWorker {

    private val applicationContext: Context get() = context

    override suspend fun doWork(input: WorkInput, env: WorkerEnvironment): WorkerResult {
        printDebug("CloudSyncWorker: Starting sync...")
        try {
            // One-shot runs (e.g. app-open sync) may skip the interval gate while
            // still honouring per-account network/availability policies.
            val ignoreInterval = input.getBoolean(KEY_IGNORE_INTERVAL, false)
            var mediaChanged = false
            var retryNeeded = false
            val isMetered = runCatching {
                (applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as? ConnectivityManager)?.isActiveNetworkMetered ?: false
            }.getOrDefault(false)
            val configs = configDao.getAll().first()
            for (config in configs) {
                if (!config.isActive || !config.syncEnabled) continue

                // Resolve the provider for THIS specific account (configId), not the first
                // instance of its type — otherwise two accounts of the same provider type
                // (e.g. two Immich servers) would both sync against whichever registered first.
                val provider = registry.getByConfigId(config.id) as? RemoteMediaProvider ?: continue
                if (!provider.isAvailable || (isMetered && !CloudCellularPolicy.allowsSync(config))) continue
                val syncProvider = provider as? SyncCapableProvider ?: continue
                val previousState = syncStateDao.get(config.providerType, config.id)
                val syncStartedAt = System.currentTimeMillis()
                if (!ignoreInterval && !isCloudSyncDue(
                        lastSyncTimestamp = previousState?.lastSyncTimestamp ?: 0L,
                        intervalMinutes = config.syncIntervalMinutes,
                        now = syncStartedAt
                    )) continue

                // Providers without a real deletion channel can't spot vanished remote
                // files in a delta; ask them for a full index on a slower cadence so the
                // cache still self-heals (deletions propagate within ~24h).
                val reconcileIndex = isCloudReconcileDue(
                    lastReconcileCursor = previousState?.lastSyncCursor,
                    now = syncStartedAt
                )

                printDebug("CloudSyncWorker: Syncing ${config.providerType.displayName} #${config.id} (reconcile=$reconcileIndex)...")
                val syncResult = syncIncrementally(
                    lastWatermark = previousState?.lastSyncTimestamp ?: 0L,
                    nextWatermark = syncStartedAt,
                    fetch = { syncProvider.getSyncDelta(it, reconcileIndex) },
                    persist = { delta ->
                        printDebug("CloudSyncWorker: ${delta.items.size} changes, ${delta.deletedRemoteIds.size} deletions for ${config.providerType.displayName} #${config.id}")
                        applyCloudSyncDelta(applicationContext, cloudMediaDao, config, delta)
                    },
                    advanceWatermark = { timestamp ->
                        // Update last sync timestamp; record when a full-index reconcile last
                        // ran so providers with an expensive index stay on the slow cadence.
                        syncStateDao.upsert(
                            (previousState ?: SyncStateEntity(
                                providerType = config.providerType,
                                serverConfigId = config.id
                            )).copy(
                                lastSyncTimestamp = timestamp,
                                lastSyncCursor = if (reconcileIndex) {
                                    timestamp.toString()
                                } else {
                                    previousState?.lastSyncCursor
                                },
                                lastError = null
                            )
                        )
                    }
                )
                syncResult.onSuccess { delta ->
                    mediaChanged = mediaChanged || delta.changedCount > 0
                    printDebug("CloudSyncWorker: Done syncing ${config.providerType.displayName}")
                }.onFailure { error ->
                    retryNeeded = true
                    printDebug("CloudSyncWorker: Sync failed for ${config.providerType.displayName} #${config.id}: ${error.message}")
                }
            }
            if (mediaChanged) {
                smartScanScheduler.automatic(SmartScanFeature.ALL_MASK)
                // New/changed cloud rows need a capture-time index pass so remote
                // media (WebDAV/SMB/NFS carry no provider capture date) gets its
                // embedded EXIF/container time resolved into media_capture_time.
                workScheduler().enqueueCaptureTimeIndex()
            }
            return if (retryNeeded) WorkerResult.Retry("one or more accounts failed")
                else WorkerResult.Success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            printDebug("CloudSyncWorker: Failed: ${e.message}")
            return WorkerResult.Retry("sync failed: ${e.message}")
        }
    }

    companion object {
        const val WORKER_NAME = "CloudSyncWorker"
        private const val WORK_NAME = "cloud_sync"
        private const val WORK_NAME_ONCE = "cloud_sync_once"
        private const val KEY_IGNORE_INTERVAL = "ignore_sync_interval"

        /**
         * Runs one remote-delta sync pass immediately (subject to network). Used by
         * the app-open sync trigger; [ignoreInterval] skips the per-account
         * [isCloudSyncDue] gate while keeping metered-network/availability checks.
         */
        suspend fun syncNow(scheduler: BackgroundTaskScheduler, ignoreInterval: Boolean = false) {
            scheduler.enqueue(
                id = WORK_NAME_ONCE,
                trigger = TaskTrigger.OneTime(),
                workerClassName = WORKER_NAME,
                constraints = Constraints(requiresNetwork = true),
                inputJson = workDataJson(KEY_IGNORE_INTERVAL to ignoreInterval),
                policy = ExistingPolicy.KEEP,
            )
        }

        suspend fun schedule(
            scheduler: BackgroundTaskScheduler,
            intervalMinutes: Long = 60,
            wifiOnly: Boolean = true
        ) {
            scheduler.enqueue(
                id = WORK_NAME,
                trigger = TaskTrigger.Periodic(
                    intervalMs = TimeUnit.MINUTES.toMillis(intervalMinutes)
                ),
                workerClassName = WORKER_NAME,
                constraints = Constraints(
                    requiresNetwork = true,
                    requiresUnmeteredNetwork = wifiOnly,
                    backoffPolicy = BackoffPolicy.EXPONENTIAL,
                    backoffDelayMs = TimeUnit.MINUTES.toMillis(5),
                ),
                policy = ExistingPolicy.UPDATE,
            )
        }

        fun cancel(scheduler: BackgroundTaskScheduler) {
            scheduler.cancel(WORK_NAME)
        }
    }
}

@Worker(name = CloudSyncWorker.WORKER_NAME)
class CloudSyncWorkerAdapter : GalleryWorkerAdapter(CloudSyncWorker.WORKER_NAME) {
    override suspend fun run(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        WorkComponentHolder.component.cloudSyncWorker.doWork(input, env)
}

/**
 * Persists a [SyncDelta] into Room: upserts changed rows, applies provider-reported
 * deletions, and prunes cached rows against a complete remote index. When the account
 * has `syncRemoteDeletions` enabled, pruned rows that carry an app-owned local copy
 * (`appLocalCopy`) also have that copy removed — user files are never touched because
 * the DAO only surfaces app-flagged copies. Shared by the periodic worker and the
 * pull-to-refresh path (`CloudRepository.syncAllRemoteChanges`).
 */
internal suspend fun applyCloudSyncDelta(
    context: Context,
    cloudMediaDao: CloudMediaDao,
    config: CloudServerConfigEntity,
    delta: SyncDelta
) {
    if (delta.items.isNotEmpty()) cloudMediaDao.insertAll(delta.items)
    val prunedLocalCopies = mutableListOf<CloudMediaLocalState>()
    if (delta.deletedRemoteIds.isNotEmpty()) {
        prunedLocalCopies += cloudMediaDao.deleteByRemoteIds(
            config.providerType, config.id, delta.deletedRemoteIds
        )
    }
    delta.completeRemoteIds?.let { remoteIds ->
        prunedLocalCopies += cloudMediaDao.deleteMissingRemoteMedia(
            config.id, config.providerType, remoteIds
        )
    }
    if (config.syncRemoteDeletions && prunedLocalCopies.isNotEmpty()) {
        deleteAppLocalCopies(context, prunedLocalCopies)
    }
}

internal fun isCloudSyncDue(
    lastSyncTimestamp: Long,
    intervalMinutes: Int,
    now: Long
): Boolean = lastSyncTimestamp <= 0L ||
    now - lastSyncTimestamp >= intervalMinutes.coerceAtLeast(15) * 60_000L

internal const val CLOUD_RECONCILE_INTERVAL_MS = 24L * 60L * 60_000L

/**
 * Full-index reconciles are expensive for providers without a real delta channel, so
 * they run on a slower cadence than regular syncs. [lastReconcileCursor] stores the
 * epoch-millis of the last successful reconcile in `sync_state.lastSyncCursor`; a
 * missing/legacy value reconciles immediately (first sync always covers the index).
 */
internal fun isCloudReconcileDue(
    lastReconcileCursor: String?,
    now: Long,
    intervalMs: Long = CLOUD_RECONCILE_INTERVAL_MS
): Boolean = lastReconcileCursor?.toLongOrNull()?.let { now - it >= intervalMs } ?: true

internal suspend fun <T> syncIncrementally(
    lastWatermark: Long,
    nextWatermark: Long,
    fetch: suspend (Long) -> Result<T>,
    persist: suspend (T) -> Unit,
    advanceWatermark: suspend (Long) -> Unit
): Result<T> {
    val fetched = try {
        fetch(lastWatermark)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        return Result.failure(error)
    }
    val changes = fetched.getOrElse { error ->
        if (error is CancellationException) throw error
        return Result.failure(error)
    }
    return try {
        persist(changes)
        advanceWatermark(nextWatermark)
        Result.success(changes)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
