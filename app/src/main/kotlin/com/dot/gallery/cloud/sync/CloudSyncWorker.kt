/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import android.content.Context
import android.net.ConnectivityManager
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.dao.SyncStateDao
import com.dot.gallery.cloud.data.entity.SyncStateEntity
import com.dot.gallery.core.smart.SmartScanScheduler
import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
import com.dot.gallery.feature_node.presentation.util.printDebug
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val registry: ProviderRegistry,
    private val configDao: CloudServerConfigDao,
    private val syncStateDao: SyncStateDao,
    private val cloudMediaDao: CloudMediaDao,
    private val smartScanScheduler: SmartScanScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        printDebug("CloudSyncWorker: Starting sync...")
        try {
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
                if (!isCloudSyncDue(
                        lastSyncTimestamp = previousState?.lastSyncTimestamp ?: 0L,
                        intervalMinutes = config.syncIntervalMinutes,
                        now = syncStartedAt
                    )) continue

                printDebug("CloudSyncWorker: Syncing ${config.providerType.displayName} #${config.id}...")
                val syncResult = syncIncrementally(
                    lastWatermark = previousState?.lastSyncTimestamp ?: 0L,
                    nextWatermark = syncStartedAt,
                    fetch = syncProvider::getChangedSince,
                    persist = { changed ->
                        printDebug("CloudSyncWorker: ${changed.size} changes for ${config.providerType.displayName} #${config.id}")
                        // Persist the delta into Room so the unified timeline reflects remote
                        // changes. Previously the changed set was fetched then discarded, which
                        // made the periodic worker a no-op beyond advancing the timestamp.
                        if (changed.isNotEmpty()) cloudMediaDao.insertAll(changed)
                    },
                    advanceWatermark = { timestamp ->
                        // Update last sync timestamp
                        syncStateDao.upsert(
                            (previousState ?: SyncStateEntity(
                                providerType = config.providerType,
                                serverConfigId = config.id
                            )).copy(lastSyncTimestamp = timestamp, lastError = null)
                        )
                    }
                )
                syncResult.onSuccess { changedCount ->
                    mediaChanged = mediaChanged || changedCount > 0
                    printDebug("CloudSyncWorker: Done syncing ${config.providerType.displayName}")
                }.onFailure { error ->
                    retryNeeded = true
                    printDebug("CloudSyncWorker: Sync failed for ${config.providerType.displayName} #${config.id}: ${error.message}")
                }
            }
            if (mediaChanged) smartScanScheduler.automatic(SmartScanFeature.ALL_MASK)
            return if (retryNeeded) Result.retry() else Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            printDebug("CloudSyncWorker: Failed: ${e.message}")
            return Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "cloud_sync"

        fun schedule(
            workManager: WorkManager,
            intervalMinutes: Long = 60,
            wifiOnly: Boolean = true
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .build()

            val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(
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

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}

internal fun isCloudSyncDue(
    lastSyncTimestamp: Long,
    intervalMinutes: Int,
    now: Long
): Boolean = lastSyncTimestamp <= 0L ||
    now - lastSyncTimestamp >= intervalMinutes.coerceAtLeast(15) * 60_000L

internal suspend fun <T> syncIncrementally(
    lastWatermark: Long,
    nextWatermark: Long,
    fetch: suspend (Long) -> Result<List<T>>,
    persist: suspend (List<T>) -> Unit,
    advanceWatermark: suspend (Long) -> Unit
): Result<Int> {
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
        Result.success(changes.size)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
