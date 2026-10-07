/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.repository

import com.dot.gallery.domain.model.Media

/**
 * App-side WorkManager orchestration for repository-driven media jobs.
 * Bound in `AppComponent`; keeps `MediaRepositoryImpl` free of WorkManager.
 */
interface MediaRepositoryRuntime {

    /** Enqueues copy workers for the given media→destination-path pairs. */
    suspend fun <T : Media> enqueueMediaCopies(sets: List<Pair<T, String>>)

    /** Schedules the capture-time index pass (debounced unique work). */
    suspend fun enqueueCaptureTimeIndex()
}
