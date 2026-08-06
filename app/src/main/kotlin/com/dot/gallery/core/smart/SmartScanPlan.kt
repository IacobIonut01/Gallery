/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
import com.dot.gallery.feature_node.data.data_source.SmartScanPhase
import com.dot.gallery.feature_node.data.data_source.SmartScanPhaseEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanStatus

data class SmartScanProgress(
    val total: Int,
    val processed: Int,
    val succeeded: Int,
    val skipped: Int,
    val failed: Int
) {
    init {
        require(total >= 0 && processed in 0..total)
        require(succeeded >= 0 && skipped >= 0 && failed >= 0)
        require(succeeded + skipped + failed <= processed)
    }

    val percent: Int
        get() = if (total <= 0) 0 else ((processed.coerceAtMost(total) * 100L) / total).toInt()

    operator fun plus(other: SmartScanProgress) = SmartScanProgress(
        total = total + other.total,
        processed = processed + other.processed,
        succeeded = succeeded + other.succeeded,
        skipped = skipped + other.skipped,
        failed = failed + other.failed
    )

    companion object {
        val EMPTY = SmartScanProgress(0, 0, 0, 0, 0)
    }
}

object SmartScanPlan {
    const val FOREGROUND_MEDIA_THRESHOLD = 1_000

    val orderedPhases = listOf(
        SmartScanPhase.SOURCE_SYNC,
        SmartScanPhase.METADATA,
        SmartScanPhase.SEARCH_INDEX,
        SmartScanPhase.CATEGORY_CLASSIFICATION,
        SmartScanPhase.FACE_INDEX
    )

    fun expandedFeatures(features: Int): Int = SmartScanFeature.expandDependencies(features)

    fun shouldProcess(fullRefresh: Boolean, isCurrent: Boolean): Boolean = fullRefresh || !isCurrent

    fun phasesFor(features: Int): List<SmartScanPhase> {
        val expanded = expandedFeatures(features)
        return buildList {
            add(SmartScanPhase.SOURCE_SYNC)
            if (expanded and SmartScanFeature.METADATA.bit != 0) add(SmartScanPhase.METADATA)
            if (expanded and SmartScanFeature.EMBEDDINGS.bit != 0) add(SmartScanPhase.SEARCH_INDEX)
            if (expanded and SmartScanFeature.CATEGORIES.bit != 0) add(SmartScanPhase.CATEGORY_CLASSIFICATION)
            if (expanded and SmartScanFeature.PERSONS.bit != 0) add(SmartScanPhase.FACE_INDEX)
        }
    }

    fun aggregate(progress: Iterable<SmartScanProgress>): SmartScanProgress =
        progress.fold(SmartScanProgress.EMPTY, SmartScanProgress::plus)

    fun estimatedRemainingMillis(
        total: Int,
        processed: Int,
        startedAt: Long?,
        now: Long = System.currentTimeMillis()
    ): Long? {
        if (startedAt == null || total <= 0 || processed <= 0 || processed >= total || now <= startedAt) return null
        val elapsed = now - startedAt
        return (((total - processed).toDouble() * elapsed) / processed)
            .toLong()
            .coerceAtLeast(0L)
    }

    fun requiresForeground(mediaCount: Int): Boolean = mediaCount > FOREGROUND_MEDIA_THRESHOLD

    fun overallProgress(phases: List<SmartScanPhaseEntity>): Float {
        if (phases.isEmpty()) return 0f
        return phases.sumOf { phase ->
            when (phase.status) {
                SmartScanStatus.SUCCEEDED,
                SmartScanStatus.PARTIAL,
                SmartScanStatus.BLOCKED,
                SmartScanStatus.FAILED,
                SmartScanStatus.CANCELLED -> 1.0
                SmartScanStatus.RUNNING -> if (phase.totalMedia <= 0) 0.0
                else phase.processedMedia.coerceIn(0, phase.totalMedia).toDouble() / phase.totalMedia
                SmartScanStatus.QUEUED,
                SmartScanStatus.INTERRUPTED -> 0.0
            }
        }.div(phases.size).toFloat().coerceIn(0f, 1f)
    }

    fun terminalStatus(statuses: Iterable<SmartScanStatus>): SmartScanStatus {
        val values = statuses.toList()
        val completed = values.count { it == SmartScanStatus.SUCCEEDED }
        val partial = values.count { it == SmartScanStatus.PARTIAL }
        val blocked = values.count { it == SmartScanStatus.BLOCKED }
        val failed = values.count { it == SmartScanStatus.FAILED }
        return when {
            partial > 0 -> SmartScanStatus.PARTIAL
            failed > 0 && completed == 0 && blocked == 0 -> SmartScanStatus.FAILED
            blocked > 0 && completed == 0 && failed == 0 -> SmartScanStatus.BLOCKED
            failed > 0 || blocked > 0 -> SmartScanStatus.PARTIAL
            values.isNotEmpty() && completed == values.size -> SmartScanStatus.SUCCEEDED
            else -> SmartScanStatus.FAILED
        }
    }
}
