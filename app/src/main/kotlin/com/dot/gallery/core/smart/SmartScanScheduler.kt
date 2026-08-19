/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dot.gallery.core.workers.SmartScanWorker
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.SmartScanDao
import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
import com.dot.gallery.feature_node.data.data_source.SmartScanPhase
import com.dot.gallery.feature_node.data.data_source.SmartScanPhaseEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanRunEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanScheduleResult
import com.dot.gallery.feature_node.data.data_source.SmartScanStatus
import com.dot.gallery.feature_node.data.data_source.SmartScanTrigger
import com.dot.gallery.feature_node.presentation.util.mediaStoreVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal fun smartScanConstraintsFor(phases: Collection<SmartScanPhase>, userVisible: Boolean): Constraints =
    Constraints.Builder()
        .setRequiresStorageNotLow(true)
        .setRequiresBatteryNotLow(!userVisible)
        .setRequiresCharging(!userVisible && phases.any(SMART_SCAN_HEAVY_PHASES::contains))
        .build()

private val SMART_SCAN_HEAVY_PHASES = setOf(
    SmartScanPhase.SEARCH_INDEX,
    SmartScanPhase.CATEGORY_CLASSIFICATION,
    SmartScanPhase.FACE_INDEX
)

@Singleton
class SmartScanScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val dao: SmartScanDao,
    private val database: InternalDatabase,
    private val processors: Provider<SmartScanProcessorRegistry>,
    @ApplicationContext private val appContext: Context
) {
    private val schedulingMutex = Mutex()

    suspend fun automatic(features: Int): SmartScanScheduleResult =
        schedule(features, SmartScanTrigger.AUTOMATIC, userVisible = false, fullRefresh = false)

    suspend fun automaticIfNeeded(features: Int): SmartScanScheduleResult? = schedulingMutex.withLock {
        val expanded = SmartScanPlan.expandedFeatures(features)
        val phases = SmartScanPlan.phasesFor(expanded)
        val featurePhases = phases.filterNot { it == SmartScanPhase.SOURCE_SYNC }
        val registry = processors.get()
        val expectedRevisions = featurePhases.associateWith { registry.processorFor(it).revision }
        val latestSourcePhase = dao.getLatestSuccessfulPhase(SmartScanPhase.SOURCE_SYNC)
        val currentSourceSnapshot = latestSourcePhase?.let { dao.getRun(it.runId)?.sourceSnapshot }
        val latestRevisions = buildMap {
            featurePhases.forEach { phase ->
                val latest = dao.getLatestCurrentPhase(phase)
                val latestRevision = latest?.processorRevision
                val phaseSourceSnapshot = latest?.let { dao.getRun(it.runId)?.sourceSnapshot }
                if (SmartScanPlan.isPhaseCheckpointCurrent(
                        expectedRevisions.getValue(phase),
                        latestRevision,
                        currentSourceSnapshot,
                        phaseSourceSnapshot
                    )
                ) {
                    put(phase, requireNotNull(latestRevision))
                }
            }
        }
        val mediaVersion = appContext.mediaStoreVersion
        val latestSourceSnapshot = coroutineScope {
            val media = async { database.getMediaDao().getMedia() }
            val cloud = async { database.getCloudMediaDao().getAllCachedAsync() }
            smartSourceSnapshot(mediaVersion, media.await(), cloud.await())
        }
        val mediaCurrent = database.getMediaDao().isMediaVersionUpToDate(mediaVersion) &&
            latestSourcePhase?.processorRevision == registry.processorFor(SmartScanPhase.SOURCE_SYNC).revision &&
            currentSourceSnapshot == latestSourceSnapshot
        if (SmartScanPlan.isAutomaticScanCurrent(mediaCurrent, expectedRevisions, latestRevisions)) null
        else scheduleLocked(expanded, SmartScanTrigger.AUTOMATIC, userVisible = false, fullRefresh = false)
    }

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
        return@withLock enqueueRunLocked(run.runId, replace = true)
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
        val plannedPhases = SmartScanPlan.phasesFor(expanded)
        val previousActiveWorkId = dao.getActiveRun()?.workId
        val now = System.currentTimeMillis()
        val runId = UUID.randomUUID().toString()
        val work = workRequest(runId, plannedPhases, userVisible)
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
        val phases = plannedPhases.map { phase ->
            SmartScanPhaseEntity(runId = runId, phase = phase, updatedAt = now)
        }
        val scheduled = dao.coalesceOrCreate(run, phases)
        val scheduledWorkId = UUID.fromString(requireNotNull(scheduled.workId))
        val existingWork = workManager.getWorkInfoByIdFlow(scheduledWorkId).first()
        if (existingWork != null && !existingWork.state.isFinished) return scheduled
        val scheduledRun = requireNotNull(dao.getRun(scheduled.runId))
        val actualWorkId = if (existingWork?.state?.isFinished == true) UUID.randomUUID() else scheduledWorkId
        if (actualWorkId != scheduledWorkId) {
            dao.attachWork(scheduled.runId, actualWorkId.toString(), System.currentTimeMillis())
        }
        val actualWork = if (scheduled.runId == runId && actualWorkId == work.id) {
            work
        } else {
            workRequest(
                scheduled.runId,
                SmartScanPlan.phasesFor(scheduledRun.requestedFeatures),
                scheduledRun.userVisible,
                actualWorkId
            )
        }
        val promoted = scheduled.runId != runId && actualWorkId == work.id
        if (promoted) {
            previousActiveWorkId?.let { value ->
                runCatching { workManager.cancelWorkById(UUID.fromString(value)) }
            }
        }
        workManager.enqueueUniqueWork(
            runWorkName(scheduled.runId),
            if (promoted) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            actualWork
        )
        return scheduled
    }

    private suspend fun enqueueRunLocked(runId: String, replace: Boolean): Boolean {
        val run = dao.getRun(runId) ?: return false
        val work = workRequest(runId, SmartScanPlan.phasesFor(run.requestedFeatures), run.userVisible)
        if (dao.attachWork(runId, work.id.toString(), System.currentTimeMillis()) != 1) return false
        workManager.enqueueUniqueWork(
            runWorkName(runId),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            work
        )
        return true
    }

    private fun workRequest(
        runId: String,
        phases: Collection<SmartScanPhase>,
        userVisible: Boolean,
        id: UUID = UUID.randomUUID()
    ): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SmartScanWorker>()
        .setId(id)
        .setInputData(workDataOf(SmartScanWorker.KEY_RUN_ID to runId))
        .setConstraints(smartScanConstraintsFor(phases, userVisible))
        .addTag(SmartScanWorker.WORK_NAME)
        .addTag(SmartScanWorker.runTag(runId))
        .build()

    private fun runWorkName(runId: String): String = "$WORK_NAME:$runId"

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
