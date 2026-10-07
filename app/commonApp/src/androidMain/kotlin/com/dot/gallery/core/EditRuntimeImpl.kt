/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import com.dot.gallery.core.work.WorkSignals
import com.dot.gallery.core.work.isFinished
import com.dot.gallery.core.workers.EditBackupWorker
import com.dot.gallery.core.workers.revertEditBackup
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.TaskState
import com.dot.gallery.feature.editor.EditRuntime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take

class EditRuntimeImpl @Inject constructor(
    private val scheduler: BackgroundTaskScheduler,
) : EditRuntime {

    override fun revertEdit(mediaId: Long): Flow<Boolean> = flow {
        val taskId = scheduler.revertEditBackup(mediaId)
        emitAll(
            WorkSignals.observe(taskId, EditBackupWorker.WORKER_NAME).mapNotNull { info ->
                when {
                    info.state is TaskState.Succeeded ->
                        info.output?.getBoolean(EditBackupWorker.KEY_SUCCESS, false) ?: false
                    info.state is TaskState.Failed || info.state is TaskState.Cancelled -> false
                    else -> null // enqueued/running — keep waiting
                }
            }.take(1)
        )
    }
}
