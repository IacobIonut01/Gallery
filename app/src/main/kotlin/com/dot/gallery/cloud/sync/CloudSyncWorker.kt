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
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.dao.SyncStateDao
import com.dot.gallery.cloud.data.entity.SyncStateEntity
import com.dot.gallery.feature_node.presentation.util.printDebug
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val registry: ProviderRegistry,
    private val configDao: CloudServerConfigDao,
    private val syncStateDao: SyncStateDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        printDebug("CloudSyncWorker: Starting sync...")
        try {
            val configs = configDao.getAll().first()
            for (config in configs) {
                if (!config.isActive || !config.syncEnabled) continue

                val provider = registry.get(config.providerType) as? RemoteMediaProvider ?: continue
                if (!provider.isAvailable) continue

                printDebug("CloudSyncWorker: Syncing ${config.providerType.displayName}...")

                val syncProvider = provider as? SyncCapableProvider
                if (syncProvider != null) {
                    val lastSync = syncStateDao.get(
                        config.providerType, config.id
                    )?.lastSyncTimestamp ?: 0L

                    val changedResult = syncProvider.getChangedSince(lastSync)
                    changedResult.onSuccess { changed ->
                        printDebug("CloudSyncWorker: ${changed.size} changes for ${config.providerType.displayName}")
                    }.onFailure { e ->
                        printDebug("CloudSyncWorker: Sync failed for ${config.providerType.displayName}: ${e.message}")
                    }
                }

                // Update last sync timestamp
                syncStateDao.upsert(
                    SyncStateEntity(
                        providerType = config.providerType,
                        serverConfigId = config.id,
                        lastSyncTimestamp = System.currentTimeMillis()
                    )
                )

                printDebug("CloudSyncWorker: Done syncing ${config.providerType.displayName}")
            }
            return Result.success()
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
