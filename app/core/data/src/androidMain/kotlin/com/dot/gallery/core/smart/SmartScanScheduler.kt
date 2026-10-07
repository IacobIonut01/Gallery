/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

import com.dot.gallery.data.dao.SmartScanScheduleResult

/**
 * Schedules SmartScan runs through WorkManager. The implementation lives in `:app`
 * (`SmartScanSchedulerImpl`); feature modules consume this surface.
 */
interface SmartScanScheduler {

    suspend fun automatic(features: Int): SmartScanScheduleResult

    suspend fun automaticIfNeeded(features: Int): SmartScanScheduleResult?

    suspend fun manual(features: Int): SmartScanScheduleResult

    suspend fun all(userVisible: Boolean = true): SmartScanScheduleResult

    suspend fun fullRefresh(): SmartScanScheduleResult

    /**
     * Run only the grouping phase over already-indexed faces — no media re-scan.
     */
    suspend fun regroupFaces(): SmartScanScheduleResult

    suspend fun resumeActiveRun(): Boolean

    suspend fun retryFailed(runId: String? = null): SmartScanScheduleResult?

    suspend fun cancel(runId: String): Boolean
}
