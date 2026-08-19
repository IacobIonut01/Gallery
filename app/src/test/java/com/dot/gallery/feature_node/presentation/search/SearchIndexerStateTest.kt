/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.search

import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
import com.dot.gallery.feature_node.data.data_source.SmartScanPhase
import com.dot.gallery.feature_node.data.data_source.SmartScanPhaseEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanRunEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanStatus
import com.dot.gallery.feature_node.data.data_source.SmartScanTrigger
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchIndexerStateTest {
    private val run = SmartScanRunEntity(
        runId = "run",
        trigger = SmartScanTrigger.AUTOMATIC,
        requestedFeatures = SmartScanFeature.ALL_MASK,
        requestedAt = 1L
    )

    @Test
    fun queuedOrPreparingSearchDoesNotShowBanner() {
        assertEquals(false, deriveSearchIndexerState(run, listOf(phase(SmartScanStatus.QUEUED))).isIndexing)
        assertEquals(false, deriveSearchIndexerState(run, listOf(phase(SmartScanStatus.RUNNING))).isIndexing)
    }

    @Test
    fun bannerShowsOnlyWhileSearchHasPendingCandidates() {
        val indexing = deriveSearchIndexerState(
            run,
            listOf(phase(SmartScanStatus.RUNNING, total = 10, processed = 3))
        )

        assertEquals(true, indexing.isIndexing)
        assertEquals(10, indexing.total)
        assertEquals(3, indexing.processed)
        assertEquals(
            false,
            deriveSearchIndexerState(
                run,
                listOf(phase(SmartScanStatus.RUNNING, total = 10, processed = 10))
            ).isIndexing
        )
    }

    @Test
    fun terminalSearchDoesNotShowBanner() {
        assertEquals(
            false,
            deriveSearchIndexerState(
                run,
                listOf(phase(SmartScanStatus.SUCCEEDED, total = 10, processed = 10))
            ).isIndexing
        )
    }

    private fun phase(
        status: SmartScanStatus,
        total: Int = 0,
        processed: Int = 0
    ) = SmartScanPhaseEntity(
        runId = run.runId,
        phase = SmartScanPhase.SEARCH_INDEX,
        status = status,
        updatedAt = 1L,
        totalMedia = total,
        processedMedia = processed
    )
}
