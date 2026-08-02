/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dot.gallery.R
import com.dot.gallery.core.smart.SmartScanPhaseContext
import com.dot.gallery.core.smart.SmartScanPhaseResult
import com.dot.gallery.core.smart.SmartScanPlan
import com.dot.gallery.core.smart.SmartScanProcessorRegistry
import com.dot.gallery.core.smart.SmartScanProgress
import com.dot.gallery.feature_node.data.data_source.SmartScanDao
import com.dot.gallery.feature_node.data.data_source.SmartScanPhase
import com.dot.gallery.feature_node.data.data_source.SmartScanPhaseEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltWorker
class SmartScanWorker @AssistedInject constructor(
    private val dao: SmartScanDao,
    private val processors: SmartScanProcessorRegistry,
    private val workManager: WorkManager,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val runId = inputData.getString(KEY_RUN_ID).orEmpty()
    private val owner = id.toString()
    private var activePhase: SmartScanPhase? = null
    private var userVisible = false

    override suspend fun doWork(): Result {
        if (runId.isBlank()) return Result.failure(workDataOf(KEY_ERROR_CODE to "missing_run_id"))
        val initial = dao.getRun(runId)
            ?: return Result.failure(workDataOf(KEY_ERROR_CODE to "run_not_found"))
        userVisible = initial.userVisible
        val now = System.currentTimeMillis()
        dao.recoverExpiredFeatureLeases(now)
        dao.recoverExpiredPhaseLeases(now)
        dao.recoverExpiredRunLeases(now)
        if (dao.claimRunLease(runId, owner, now, now + LEASE_MILLIS) != 1) {
            return resultFor(initial.status)
        }
        if (userVisible) setForeground(foregroundInfo(0, null))

        return try {
            dispatch()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { persistInterruption() }
            throw cancelled
        } catch (error: Throwable) {
            val code = error.javaClass.simpleName.ifBlank { "dispatcher_failed" }
            withContext(NonCancellable) {
                activePhase?.let { dao.finishPhaseOwned(runId, it, owner, SmartScanStatus.FAILED, System.currentTimeMillis(), code) }
                dao.finishRunOwned(runId, owner, SmartScanStatus.FAILED, System.currentTimeMillis(), code)
            }
            Result.failure(workDataOf(KEY_ERROR_CODE to code))
        }
    }

    private suspend fun dispatch(): Result {
        while (currentCoroutineContext().isActive) {
            val run = dao.getRun(runId) ?: return Result.failure()
            if (run.status == SmartScanStatus.CANCELLED) return Result.failure()
            val planned = SmartScanPlan.phasesFor(run.requestedFeatures)
            val phases = dao.getPhases(runId).associateBy { it.phase }
            val next = planned.firstOrNull { phase ->
                phases[phase]?.status in setOf(SmartScanStatus.QUEUED, SmartScanStatus.RUNNING)
            } ?: break
            val processor = processors.processorFor(next)
            val now = System.currentTimeMillis()
            if (dao.claimPhaseLease(runId, next, owner, now, now + LEASE_MILLIS, processor.revision) != 1) {
                return Result.retry()
            }
            activePhase = next
            check(dao.updateCurrentPhase(runId, owner, next, run.sourceSnapshot, now) == 1) {
                "run_lease_lost"
            }
            val outcome = runProcessorWithHeartbeat(processor, run.sourceSnapshot, run.fullRefresh)
            finishPhase(next, outcome)
            activePhase = null
        }

        val terminalPhases = dao.getPhases(runId)
            .filter { it.phase in SmartScanPlan.phasesFor(dao.getRun(runId)?.requestedFeatures ?: 0) }
        val requestedFeaturePhases = terminalPhases.filter { it.phase != SmartScanPhase.SOURCE_SYNC }
        val status = SmartScanPlan.terminalStatus(requestedFeaturePhases.map { it.status })
        val error = requestedFeaturePhases.firstNotNullOfOrNull { it.lastErrorCode }
        val finished = dao.finishRunIfComplete(runId, owner, status, System.currentTimeMillis(), error)
        if (finished != 1) return dispatch()
        publishAggregate()
        return resultFor(status)
    }

    private suspend fun runProcessorWithHeartbeat(
        processor: com.dot.gallery.core.smart.SmartScanPhaseProcessor,
        sourceSnapshot: String,
        fullRefresh: Boolean
    ): SmartScanPhaseResult = coroutineScope {
        val heartbeat = launch {
            while (isActive) {
                delay(HEARTBEAT_MILLIS)
                renewLeases(processor.phase)
            }
        }
        try {
            processor.process(
                SmartScanPhaseContext(runId, sourceSnapshot, fullRefresh) { progress ->
                    persistProgress(processor.phase, progress)
                }
            )
        } finally {
            heartbeat.cancelAndJoin()
        }
    }

    private suspend fun finishPhase(phase: SmartScanPhase, result: SmartScanPhaseResult) {
        persistProgress(phase, result.progress)
        val now = System.currentTimeMillis()
        when (result) {
            is SmartScanPhaseResult.Completed -> {
                result.sourceSnapshot?.let { snapshot ->
                    dao.updateCurrentPhase(runId, owner, phase, snapshot, now)
                }
                dao.finishPhaseOwned(runId, phase, owner, SmartScanStatus.SUCCEEDED, now)
            }
            is SmartScanPhaseResult.Partial ->
                dao.finishPhaseOwned(runId, phase, owner, SmartScanStatus.PARTIAL, now, result.errorCode)
            is SmartScanPhaseResult.Blocked ->
                dao.finishPhaseOwned(runId, phase, owner, SmartScanStatus.BLOCKED, now, result.errorCode)
            is SmartScanPhaseResult.Failed ->
                dao.finishPhaseOwned(runId, phase, owner, SmartScanStatus.FAILED, now, result.errorCode)
        }
        publishAggregate()
    }

    private suspend fun persistProgress(phase: SmartScanPhase, progress: SmartScanProgress) {
        currentCoroutineContext().ensureActive()
        renewLeases(phase)
        dao.updatePhaseSummary(
            runId,
            phase,
            progress.total,
            progress.processed,
            progress.succeeded,
            progress.skipped,
            progress.failed,
            System.currentTimeMillis()
        )
        publishAggregate()
    }

    private suspend fun publishAggregate() {
        val phases = dao.getPhases(runId)
        val aggregate = SmartScanPlan.aggregate(phases.map { it.toProgress() })
        val now = System.currentTimeMillis()
        dao.updateRunSummary(
            runId,
            aggregate.total,
            aggregate.processed,
            aggregate.succeeded,
            aggregate.skipped,
            aggregate.failed,
            now
        )
        setProgress(
            workDataOf(
                KEY_PROGRESS to aggregate.percent,
                KEY_TOTAL to aggregate.total,
                KEY_PROCESSED to aggregate.processed,
                KEY_PHASE to activePhase?.storedValue
            )
        )
        if (userVisible) setForeground(foregroundInfo(aggregate.percent, activePhase))
    }

    private suspend fun renewLeases(phase: SmartScanPhase) {
        val now = System.currentTimeMillis()
        check(dao.renewRunLease(runId, owner, now, now + LEASE_MILLIS) == 1) { "run_lease_lost" }
        check(dao.renewPhaseLease(runId, phase, owner, now, now + LEASE_MILLIS) == 1) { "phase_lease_lost" }
    }

    private suspend fun persistInterruption() {
        val run = dao.getRun(runId) ?: return
        if (run.status == SmartScanStatus.CANCELLED) return
        val now = System.currentTimeMillis()
        if (stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP) {
            dao.cancelRun(runId, now)
        } else {
            activePhase?.let { dao.releasePhaseLease(runId, it, owner, now) }
            dao.releaseRunLease(runId, owner, now)
        }
    }

    private fun foregroundInfo(progress: Int, phase: SmartScanPhase?): ForegroundInfo {
        val manager = appContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    appContext.getString(R.string.smart_scan_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(appContext.getString(R.string.smart_scan_notification_title))
            .setContentText(phase?.let { appContext.getString(R.string.smart_scan_notification_progress, progress) })
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                0,
                appContext.getString(android.R.string.cancel),
                workManager.createCancelPendingIntent(id)
            )
            .build()
        val type = if (Build.VERSION.SDK_INT >= 35) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }
        return ForegroundInfo(NOTIFICATION_ID, notification, type)
    }

    private fun resultFor(status: SmartScanStatus): Result = when (status) {
        SmartScanStatus.SUCCEEDED, SmartScanStatus.PARTIAL, SmartScanStatus.BLOCKED -> Result.success()
        SmartScanStatus.QUEUED, SmartScanStatus.RUNNING -> Result.retry()
        SmartScanStatus.FAILED, SmartScanStatus.INTERRUPTED, SmartScanStatus.CANCELLED -> Result.failure()
    }

    private fun SmartScanPhaseEntity.toProgress() = SmartScanProgress(
        totalMedia,
        processedMedia,
        succeededMedia,
        skippedMedia,
        failedMedia
    )

    companion object {
        const val WORK_NAME = "smart_scan_worker"
        const val KEY_RUN_ID = "run_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val KEY_PROCESSED = "processed"
        const val KEY_PHASE = "phase"
        const val KEY_ERROR_CODE = "error_code"
        private const val CHANNEL_ID = "smart_scan_media_processing"
        private const val NOTIFICATION_ID = 0x5343414E
        private const val LEASE_MILLIS = 5 * 60 * 1000L
        private const val HEARTBEAT_MILLIS = 60 * 1000L

        fun runTag(runId: String) = "smart_scan_run_$runId"
    }
}
