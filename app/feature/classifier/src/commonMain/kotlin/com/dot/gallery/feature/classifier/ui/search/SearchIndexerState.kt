/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui.search

import com.dot.gallery.core.smart.SmartScanPlan
import com.dot.gallery.data.entity.SmartScanFeature
import com.dot.gallery.data.entity.SmartScanPhase
import com.dot.gallery.data.entity.SmartScanPhaseEntity
import com.dot.gallery.data.entity.SmartScanRunEntity
import com.dot.gallery.data.entity.SmartScanStatus


data class SearchIndexerState(
    val isIndexing: Boolean = false,
    val status: SmartScanStatus? = null,
    val progress: Float = 0f,
    val processed: Int = 0,
    val total: Int = 0,
    val stageNumber: Int = 0,
    val stageCount: Int = 0,
    val estimatedRemainingMillis: Long? = null
)

fun deriveSearchIndexerState(
    run: SmartScanRunEntity?,
    phases: List<SmartScanPhaseEntity>
): SearchIndexerState {
    if (run == null || !SmartScanPlan.runRequestsFeature(run, SmartScanFeature.EMBEDDINGS)) {
        return SearchIndexerState()
    }
    val searchPhase = phases.firstOrNull { it.phase == SmartScanPhase.SEARCH_INDEX }
        ?.takeIf {
            it.status == SmartScanStatus.RUNNING &&
                it.totalMedia > 0 && it.processedMedia < it.totalMedia
        } ?: return SearchIndexerState()
    val planned = SmartScanPlan.phasesFor(run.requestedFeatures)
    return SearchIndexerState(
        isIndexing = true,
        status = searchPhase.status,
        progress = (searchPhase.processedMedia.toFloat() / searchPhase.totalMedia).coerceIn(0f, 1f),
        processed = searchPhase.processedMedia,
        total = searchPhase.totalMedia,
        stageNumber = planned.indexOf(SmartScanPhase.SEARCH_INDEX) + 1,
        stageCount = planned.size,
        estimatedRemainingMillis = SmartScanPlan.estimatedRemainingMillis(
            searchPhase.totalMedia,
            searchPhase.processedMedia,
            searchPhase.startedAt
        )
    )
}
