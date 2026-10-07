/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import com.dot.gallery.core.work.WorkSignals
import com.dot.gallery.core.work.isFinished
import com.dot.gallery.core.workers.EditBackupWorker
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.TaskState
import com.dot.gallery.core.workers.autoCleanupEditBackups
import com.dot.gallery.core.workers.deleteAllEditBackups
import com.dot.gallery.core.workers.deleteSelectedEditBackups
import com.dot.gallery.feature.settings.subsettings.EditBackupsRuntime
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class EditBackupsRuntimeImpl @Inject constructor(
    private val scheduler: BackgroundTaskScheduler,
) : EditBackupsRuntime {

    override suspend fun autoCleanupEditBackups(): Int {
        val id = scheduler.autoCleanupEditBackups()
        val info = WorkSignals.observe(id, EditBackupWorker.WORKER_NAME)
            .first { it.isFinished }
        return when (info.state) {
            is TaskState.Succeeded ->
                info.output?.getInt(EditBackupWorker.KEY_DELETED_COUNT, 0) ?: 0
            else -> 0
        }
    }

    override suspend fun deleteAllEditBackups(): Boolean {
        val id = scheduler.deleteAllEditBackups()
        return WorkSignals.observe(id, EditBackupWorker.WORKER_NAME)
            .first { it.isFinished }
            .state is TaskState.Succeeded
    }

    override suspend fun deleteSelectedEditBackups(mediaIds: LongArray): Boolean {
        val id = scheduler.deleteSelectedEditBackups(mediaIds)
        return WorkSignals.observe(id, EditBackupWorker.WORKER_NAME)
            .first { it.isFinished }
            .state is TaskState.Succeeded
    }
}
