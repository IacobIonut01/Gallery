/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dot.gallery.core.workers.SmartScanWorker
import com.dot.gallery.feature_node.data.data_source.SmartScanDao
import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
import com.dot.gallery.feature_node.data.data_source.SmartScanPhaseEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanRunEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanScheduleResult
import com.dot.gallery.feature_node.data.data_source.SmartScanStatus
import com.dot.gallery.feature_node.data.data_source.SmartScanTrigger
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartScanScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val dao: SmartScanDao
) {
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

    suspend fun cancel(runId: String): Boolean {
        val run = dao.cancelRun(runId, System.currentTimeMillis()) ?: return false
        run.workId?.let { value ->
            runCatching { workManager.cancelWorkById(UUID.fromString(value)) }
        }
        return true
    }

    private suspend fun schedule(
        features: Int,
        trigger: SmartScanTrigger,
        userVisible: Boolean,
        fullRefresh: Boolean
    ): SmartScanScheduleResult {
        require(features and SmartScanFeature.ALL_MASK != 0) { "At least one Smart Scan feature is required" }
        require(features and SmartScanFeature.ALL_MASK.inv() == 0) { "Unknown Smart Scan feature bits" }

        val expanded = SmartScanPlan.expandedFeatures(features)
        val now = System.currentTimeMillis()
        val runId = UUID.randomUUID().toString()
        val work = OneTimeWorkRequestBuilder<SmartScanWorker>()
            .setId(UUID.randomUUID())
            .setInputData(workDataOf(SmartScanWorker.KEY_RUN_ID to runId))
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .addTag(SmartScanWorker.WORK_NAME)
            .addTag(SmartScanWorker.runTag(runId))
            .build()
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
        val actualWork = if (scheduled.runId == runId && scheduledWorkId == work.id) work else
            OneTimeWorkRequestBuilder<SmartScanWorker>()
                .setId(scheduledWorkId)
                .setInputData(workDataOf(SmartScanWorker.KEY_RUN_ID to scheduled.runId))
                .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
                .addTag(SmartScanWorker.WORK_NAME)
                .addTag(SmartScanWorker.runTag(scheduled.runId))
                .build()
        workManager.enqueueUniqueWork(
            "$WORK_NAME:${scheduled.runId}",
            ExistingWorkPolicy.KEEP,
            actualWork
        )
        return scheduled
    }

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
