/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dot.gallery.core.workers.SmartScanWorker
import com.dot.gallery.feature_node.data.data_source.SmartScanDao
import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
import com.dot.gallery.feature_node.data.data_source.SmartScanPhase
import com.dot.gallery.feature_node.data.data_source.SmartScanPhaseEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanRunEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanScheduleResult
import com.dot.gallery.feature_node.data.data_source.SmartScanStatus
import com.dot.gallery.feature_node.data.data_source.SmartScanTrigger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal fun smartScanConstraintsFor(phase: SmartScanPhase, userVisible: Boolean): Constraints =
    Constraints.Builder()
        .setRequiresStorageNotLow(true)
        .setRequiresBatteryNotLow(!userVisible)
        .setRequiresCharging(!userVisible && phase in SMART_SCAN_HEAVY_PHASES)
        .build()

private val SMART_SCAN_HEAVY_PHASES = setOf(
    SmartScanPhase.SEARCH_INDEX,
    SmartScanPhase.CATEGORY_CLASSIFICATION,
    SmartScanPhase.FACE_INDEX
)

@Singleton
class SmartScanScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val dao: SmartScanDao
) {
    private val schedulingMutex = Mutex()

    suspend fun automatic(features: Int): SmartScanScheduleResult =
        schedule(features, SmartScanTrigger.AUTOMATIC, userVisible = false, fullRefresh = false)

    suspend fun manual(features: Int): SmartScanScheduleResult =
        schedule(features, SmartScanTrigger.MANUAL, userVisible = true, fullRefresh = false)

    suspend fun all(userVisible: Boolean = true): SmartScanScheduleResult = schedule(
        features = SmartScanFeature.ALL_MASK,
        trigger = if (userVisible) SmartScanTrigger.MANUAL else SmartScanTrigger.AUTOMATIC,
        userVisible = userVisible,
        fullRefresh = false
    )

    suspend fun fullRefresh(): SmartScanScheduleResult = schedule(
        features = SmartScanFeature.ALL_MASK,
        trigger = SmartScanTrigger.MANUAL,
        userVisible = true,
        fullRefresh = true
    )

    suspend fun resumeActiveRun(): Boolean = schedulingMutex.withLock {
        val run = dao.getActiveRun() ?: return@withLock false
        val workId = run.workId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val workInfo = workId?.let { workManager.getWorkInfoByIdFlow(it).first() }
        if (workInfo != null && !workInfo.state.isFinished) return@withLock true

        if (!dao.prepareRunRecovery(run.runId, UUID.randomUUID().toString(), System.currentTimeMillis())) {
            return@withLock false
        }
        return@withLock enqueueNextPhaseLocked(run.runId, replace = true)
    }

    suspend fun retryFailed(runId: String? = null): SmartScanScheduleResult? {
        val failed = if (runId != null) dao.getRun(runId) else dao.getLatestRetryableRun()
        failed ?: return null
        if (failed.status !in RETRYABLE_STATUSES) return null
        return schedule(
            features = failed.requestedFeatures,
            trigger = SmartScanTrigger.RECOVERY,
            userVisible = failed.userVisible,
            fullRefresh = failed.fullRefresh
        )
    }

    suspend fun cancel(runId: String): Boolean = schedulingMutex.withLock {
        dao.cancelRun(runId, System.currentTimeMillis()) ?: return@withLock false
        workManager.cancelAllWorkByTag(SmartScanWorker.runTag(runId))
        true
    }

    private suspend fun schedule(
        features: Int,
        trigger: SmartScanTrigger,
        userVisible: Boolean,
        fullRefresh: Boolean
    ): SmartScanScheduleResult = schedulingMutex.withLock {
        scheduleLocked(features, trigger, userVisible, fullRefresh)
    }

    private suspend fun scheduleLocked(
        features: Int,
        trigger: SmartScanTrigger,
        userVisible: Boolean,
        fullRefresh: Boolean
    ): SmartScanScheduleResult {
        require(features and SmartScanFeature.ALL_MASK != 0) { "At least one Smart Scan feature is required" }
        require(features and SmartScanFeature.ALL_MASK.inv() == 0) { "Unknown Smart Scan feature bits" }

        val expanded = SmartScanPlan.expandedFeatures(features)
        val previousActiveWorkId = dao.getActiveRun()?.workId
        val now = System.currentTimeMillis()
        val runId = UUID.randomUUID().toString()
        val firstPhase = SmartScanPlan.phasesFor(expanded).first()
        val work = workRequest(runId, firstPhase, userVisible)
        val run = SmartScanRunEntity(
            runId = runId,
            trigger = trigger,
            requestedFeatures = expanded,
            userVisible = userVisible,
            fullRefresh = fullRefresh,
            workId = work.id.toString(),
            requestedAt = now,
            updatedAt = now
        )
        val phases = SmartScanPlan.phasesFor(expanded).map { phase ->
            SmartScanPhaseEntity(runId = runId, phase = phase, updatedAt = now)
        }
        val scheduled = dao.coalesceOrCreate(run, phases)
        val scheduledWorkId = UUID.fromString(requireNotNull(scheduled.workId))
        val existingWork = workManager.getWorkInfoByIdFlow(scheduledWorkId).first()
        if (existingWork != null && !existingWork.state.isFinished) return scheduled
        val scheduledUserVisible = dao.getRun(scheduled.runId)?.userVisible ?: userVisible
        val scheduledPhase = nextRunnablePhase(scheduled.runId) ?: firstPhase
        val actualWorkId = if (existingWork?.state?.isFinished == true) UUID.randomUUID() else scheduledWorkId
        if (actualWorkId != scheduledWorkId) {
            dao.attachWork(scheduled.runId, actualWorkId.toString(), System.currentTimeMillis())
        }
        val actualWork = if (scheduled.runId == runId && actualWorkId == work.id && scheduledPhase == firstPhase) {
            work
        } else {
            workRequest(scheduled.runId, scheduledPhase, scheduledUserVisible, actualWorkId)
        }
        val promoted = scheduled.runId != runId && actualWorkId == work.id
        if (promoted) {
            previousActiveWorkId?.let { value ->
                runCatching { workManager.cancelWorkById(UUID.fromString(value)) }
            }
        }
        val policy = if (promoted) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        workManager.enqueueUniqueWork(
            phaseWorkName(scheduled.runId, scheduledPhase),
            policy,
            actualWork
        )
        return scheduled
    }

    suspend fun enqueueNextPhase(
        runId: String,
        replace: Boolean = false,
        completedWorkId: String? = null
    ): Boolean = schedulingMutex.withLock {
        enqueueNextPhaseLocked(runId, replace, completedWorkId)
    }

    private suspend fun enqueueNextPhaseLocked(
        runId: String,
        replace: Boolean,
        completedWorkId: String? = null
    ): Boolean {
        val run = dao.getRun(runId) ?: return false
        val phase = nextRunnablePhase(runId) ?: return false
        val existingId = run.workId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val existing = existingId?.let { workManager.getWorkInfoByIdFlow(it).first() }
        if (!replace && existingId?.toString() != completedWorkId && existing != null && !existing.state.isFinished) {
            return true
        }
        val work = workRequest(runId, phase, run.userVisible)
        if (dao.attachWork(runId, work.id.toString(), System.currentTimeMillis()) != 1) return false
        workManager.enqueueUniqueWork(
            phaseWorkName(runId, phase, work.id),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            work
        )
        return true
    }

    private suspend fun nextRunnablePhase(runId: String): SmartScanPhase? {
        val run = dao.getRun(runId) ?: return null
        val phases = dao.getPhases(runId).associateBy { it.phase }
        return SmartScanPlan.phasesFor(run.requestedFeatures).firstOrNull {
            phases[it]?.status in setOf(SmartScanStatus.QUEUED, SmartScanStatus.RUNNING)
        }
    }

    private fun workRequest(
        runId: String,
        phase: SmartScanPhase,
        userVisible: Boolean,
        id: UUID = UUID.randomUUID()
    ): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SmartScanWorker>()
        .setId(id)
        .setInputData(
            workDataOf(
                SmartScanWorker.KEY_RUN_ID to runId,
                SmartScanWorker.KEY_PHASE to phase.storedValue
            )
        )
        .setConstraints(smartScanConstraintsFor(phase, userVisible))
        .addTag(SmartScanWorker.WORK_NAME)
        .addTag(SmartScanWorker.runTag(runId))
        .build()

    private fun phaseWorkName(runId: String, phase: SmartScanPhase, workId: UUID? = null): String =
        "$WORK_NAME:$runId:${phase.storedValue}${workId?.let { ":$it" }.orEmpty()}"

    companion object {
        const val WORK_NAME = "smart_scan_dispatcher"
        private val RETRYABLE_STATUSES = setOf(
            SmartScanStatus.FAILED,
            SmartScanStatus.PARTIAL,
            SmartScanStatus.BLOCKED,
            SmartScanStatus.INTERRUPTED
        )
    }
}
