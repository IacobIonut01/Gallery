/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.workers

import android.content.Context
import com.dot.gallery.core.work.WorkComponentHolder
import com.dot.gallery.core.work.GalleryWorker
import com.dot.gallery.core.work.GalleryWorkerAdapter
import com.dot.gallery.core.work.WorkInput
import com.dot.gallery.core.work.WorkSignals
import com.dot.gallery.core.work.workData
import com.dot.gallery.core.work.workDataJson
import com.dot.gallery.core.EditBackupManager
import com.dot.gallery.core.logging.withLogScope
import dev.brewkits.kmpworkmanager.annotations.Worker
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.core.logging.printError
import kotlinx.serialization.json.JsonObject
import me.tatarka.inject.annotations.Inject


/**
 * kmpworkmanager-backed worker for heavy edit-backup I/O operations:
 *  - REVERT: stream-copy the original back to the media URI
 *  - AUTO_CLEANUP: delete backups older than 90 days
 *  - DELETE_SELECTED: delete specific backups by media IDs
 *  - DELETE_ALL: delete every backup
 */
class EditBackupWorker @Inject constructor(
    private val appContext: Context,
    private val editBackupManager: EditBackupManager
) : GalleryWorker {

    override suspend fun doWork(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        withLogScope("worker.edit-backup") {
            withContext(Dispatchers.IO) {
                val op = input.getString(KEY_OPERATION)
                    ?: return@withContext failure("Missing operation")

                try {
                    when (op) {
                        OP_REVERT -> {
                            val mediaId = input.getLong(KEY_MEDIA_ID, -1L)
                            if (mediaId == -1L) return@withContext failure("Missing media ID")
                            printDebug("EditBackupWorker: reverting mediaId=$mediaId")
                            val success = editBackupManager.revertToOriginal(mediaId)
                            if (success) {
                                success("Reverted mediaId=$mediaId")
                            } else {
                                failure("Revert failed for mediaId=$mediaId")
                            }
                        }

                        OP_AUTO_CLEANUP -> {
                            val maxAgeMs = input.getLong(
                                KEY_MAX_AGE_MS, EditBackupManager.AUTO_CLEANUP_AGE_MS
                            )
                            printDebug("EditBackupWorker: auto-cleanup older than ${maxAgeMs}ms")
                            val count = editBackupManager.deleteBackupsOlderThan(maxAgeMs)
                            success(
                                "Cleaned up $count backup(s)",
                                extra = workData(KEY_DELETED_COUNT to count)
                            )
                        }

                        OP_DELETE_SELECTED -> {
                            val ids = input.getLongArray(KEY_MEDIA_IDS)
                                ?: return@withContext failure("Missing media IDs")
                            printDebug("EditBackupWorker: deleting ${ids.size} selected backup(s)")
                            editBackupManager.deleteBackups(ids.toList())
                            success("Deleted ${ids.size} backup(s)")
                        }

                        OP_DELETE_ALL -> {
                            printDebug("EditBackupWorker: deleting all backups")
                            editBackupManager.deleteAllBackups()
                            success("All backups deleted")
                        }

                        else -> failure("Unknown operation: $op")
                    }
                } catch (e: Exception) {
                    printError("EditBackupWorker failed ($op): ${e.message}")
                    failure("Error: ${e.message}")
                }
            }
        }

    private fun success(msg: String, extra: JsonObject? = null): WorkerResult =
        WorkerResult.Success(
            message = msg,
            data = workData(
                KEY_SUCCESS to true,
                KEY_MESSAGE to msg,
            ).let { base ->
                if (extra == null) base
                else JsonObject(base + extra)
            }
        )

    private fun failure(msg: String): WorkerResult {
        // Failure can't carry data in kmpworkmanager — publish the payload so
        // WorkSignals.observe(taskId, WORKER_NAME) readers see it like outputData.
        WorkSignals.recordOutput(
            WORKER_NAME,
            workData(KEY_SUCCESS to false, KEY_MESSAGE to msg)
        )
        return WorkerResult.Failure(message = msg)
    }

    companion object {
        const val TAG = "EditBackupWorker"
        const val WORKER_NAME = "EditBackupWorker"

        const val KEY_OPERATION = "operation"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_MEDIA_IDS = "media_ids"
        const val KEY_MAX_AGE_MS = "max_age_ms"
        const val KEY_SUCCESS = "success"
        const val KEY_MESSAGE = "message"
        const val KEY_DELETED_COUNT = "deleted_count"

        const val OP_REVERT = "revert"
        const val OP_AUTO_CLEANUP = "auto_cleanup"
        const val OP_DELETE_SELECTED = "delete_selected"
        const val OP_DELETE_ALL = "delete_all"
    }
}

@Worker(name = EditBackupWorker.WORKER_NAME)
class EditBackupWorkerAdapter : GalleryWorkerAdapter(EditBackupWorker.WORKER_NAME) {
    override suspend fun run(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        WorkComponentHolder.component.editBackupWorker.doWork(input, env)
}

// ─── Scheduler extension helpers ───

/**
 * Enqueues a one-shot [EditBackupWorker] task and returns its task id — the string the
 * caller later passes to `observeTaskState`/`WorkSignals.observe`.
 */
suspend fun BackgroundTaskScheduler.revertEditBackup(mediaId: Long): String {
    val id = "${EditBackupWorker.TAG}_revert_${UUID.randomUUID()}"
    enqueue(
        id = id,
        trigger = TaskTrigger.OneTime(),
        workerClassName = EditBackupWorker.WORKER_NAME,
        inputJson = workDataJson(
            EditBackupWorker.KEY_OPERATION to EditBackupWorker.OP_REVERT,
            EditBackupWorker.KEY_MEDIA_ID to mediaId
        ),
        tags = setOf(EditBackupWorker.TAG),
    )
    return id
}

suspend fun BackgroundTaskScheduler.autoCleanupEditBackups(
    maxAgeMs: Long = EditBackupManager.AUTO_CLEANUP_AGE_MS
): String {
    val id = "${EditBackupWorker.TAG}_cleanup_${UUID.randomUUID()}"
    enqueue(
        id = id,
        trigger = TaskTrigger.OneTime(),
        workerClassName = EditBackupWorker.WORKER_NAME,
        inputJson = workDataJson(
            EditBackupWorker.KEY_OPERATION to EditBackupWorker.OP_AUTO_CLEANUP,
            EditBackupWorker.KEY_MAX_AGE_MS to maxAgeMs
        ),
        tags = setOf(EditBackupWorker.TAG),
    )
    return id
}

suspend fun BackgroundTaskScheduler.deleteSelectedEditBackups(mediaIds: LongArray): String {
    val id = "${EditBackupWorker.TAG}_delete_${UUID.randomUUID()}"
    enqueue(
        id = id,
        trigger = TaskTrigger.OneTime(),
        workerClassName = EditBackupWorker.WORKER_NAME,
        inputJson = workDataJson(
            EditBackupWorker.KEY_OPERATION to EditBackupWorker.OP_DELETE_SELECTED,
            EditBackupWorker.KEY_MEDIA_IDS to mediaIds
        ),
        tags = setOf(EditBackupWorker.TAG),
    )
    return id
}

suspend fun BackgroundTaskScheduler.deleteAllEditBackups(): String {
    val id = "${EditBackupWorker.TAG}_delete_all_${UUID.randomUUID()}"
    enqueue(
        id = id,
        trigger = TaskTrigger.OneTime(),
        workerClassName = EditBackupWorker.WORKER_NAME,
        inputJson = workDataJson(
            EditBackupWorker.KEY_OPERATION to EditBackupWorker.OP_DELETE_ALL
        ),
        tags = setOf(EditBackupWorker.TAG),
    )
    return id
}
