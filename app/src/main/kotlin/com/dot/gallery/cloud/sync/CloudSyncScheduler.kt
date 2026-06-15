/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import androidx.work.WorkManager
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.feature_node.presentation.util.printDebug
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val configDao: CloudServerConfigDao
) {

    suspend fun scheduleIfNeeded() {
        val configs = configDao.getAll().first()
        val syncConfigs = configs.filter { it.isActive && it.syncEnabled }

        if (syncConfigs.isEmpty()) {
            printDebug("CloudSyncScheduler: No sync-enabled configs, canceling workers")
            CloudSyncWorker.cancel(workManager)
            CloudUploadWorker.cancel(workManager)
            return
        }

        val wifiOnly = syncConfigs.all { it.wifiOnly }
        printDebug("CloudSyncScheduler: Scheduling sync + upload (wifiOnly=$wifiOnly)")
        CloudSyncWorker.schedule(workManager, wifiOnly = wifiOnly)
        CloudUploadWorker.schedule(workManager, wifiOnly = wifiOnly)
    }

    fun cancel() {
        CloudSyncWorker.cancel(workManager)
        CloudUploadWorker.cancel(workManager)
    }
}
