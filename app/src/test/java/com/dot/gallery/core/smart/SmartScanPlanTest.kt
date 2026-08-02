/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
import com.dot.gallery.feature_node.data.data_source.SmartScanPhase
import com.dot.gallery.feature_node.data.data_source.SmartScanStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartScanPlanTest {
    @Test
    fun categoriesExpandToEmbeddings() {
        val expanded = SmartScanPlan.expandedFeatures(SmartScanFeature.CATEGORIES.bit)

        assertTrue(expanded and SmartScanFeature.CATEGORIES.bit != 0)
        assertTrue(expanded and SmartScanFeature.EMBEDDINGS.bit != 0)
    }

    @Test
    fun phasesAreAlwaysSerializedInCanonicalOrder() {
        assertEquals(
            listOf(
                SmartScanPhase.SOURCE_SYNC,
                SmartScanPhase.METADATA,
                SmartScanPhase.SEARCH_INDEX,
                SmartScanPhase.CATEGORY_CLASSIFICATION,
                SmartScanPhase.FACE_INDEX
            ),
            SmartScanPlan.phasesFor(SmartScanFeature.ALL_MASK)
        )
        assertEquals(
            listOf(SmartScanPhase.SOURCE_SYNC, SmartScanPhase.FACE_INDEX),
            SmartScanPlan.phasesFor(SmartScanFeature.PERSONS.bit)
        )
    }

    @Test
    fun incrementalSkipsCurrentItemsButFullRefreshProcessesThem() {
        assertEquals(false, SmartScanPlan.shouldProcess(fullRefresh = false, isCurrent = true))
        assertEquals(true, SmartScanPlan.shouldProcess(fullRefresh = false, isCurrent = false))
        assertEquals(true, SmartScanPlan.shouldProcess(fullRefresh = true, isCurrent = true))
    }

    @Test
    fun progressAggregatesAcrossPhases() {
        val result = SmartScanPlan.aggregate(
            listOf(
                SmartScanProgress(10, 10, 8, 1, 1),
                SmartScanProgress(20, 5, 4, 0, 1)
            )
        )

        assertEquals(SmartScanProgress(30, 15, 12, 1, 2), result)
        assertEquals(50, result.percent)
    }

    @Test
    fun terminalStatusPreservesPartialBlockedAndFailedMeaning() {
        assertEquals(
            SmartScanStatus.SUCCEEDED,
            SmartScanPlan.terminalStatus(listOf(SmartScanStatus.SUCCEEDED, SmartScanStatus.SUCCEEDED))
        )
        assertEquals(
            SmartScanStatus.BLOCKED,
            SmartScanPlan.terminalStatus(listOf(SmartScanStatus.BLOCKED))
        )
        assertEquals(
            SmartScanStatus.FAILED,
            SmartScanPlan.terminalStatus(listOf(SmartScanStatus.FAILED))
        )
        assertEquals(
            SmartScanStatus.PARTIAL,
            SmartScanPlan.terminalStatus(listOf(SmartScanStatus.SUCCEEDED, SmartScanStatus.BLOCKED))
        )
        assertEquals(
            SmartScanStatus.PARTIAL,
            SmartScanPlan.terminalStatus(listOf(SmartScanStatus.SUCCEEDED, SmartScanStatus.FAILED))
        )
    }
}
