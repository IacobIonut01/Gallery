/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import com.dot.gallery.core.workers.copyMedia
import com.dot.gallery.core.workers.enqueueCaptureTimeIndex
import com.dot.gallery.data.repository.MediaRepositoryRuntime
import com.dot.gallery.domain.model.Media
import javax.inject.Inject

class MediaRepositoryRuntimeImpl @Inject constructor(
    private val scheduler: BackgroundTaskScheduler,
) : MediaRepositoryRuntime {

    override suspend fun <T : Media> enqueueMediaCopies(sets: List<Pair<T, String>>) {
        scheduler.copyMedia(*sets.toTypedArray())
    }

    override suspend fun enqueueCaptureTimeIndex() = scheduler.enqueueCaptureTimeIndex()
}
