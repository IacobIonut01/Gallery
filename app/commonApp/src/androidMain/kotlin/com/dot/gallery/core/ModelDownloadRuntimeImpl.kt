/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.workers.cancelModelDownload
import com.dot.gallery.core.workers.downloadModels
import com.dot.gallery.feature.settings.subsettings.ModelDownloadRuntime
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton

@Singleton
class ModelDownloadRuntimeImpl @Inject constructor(
    private val scheduler: BackgroundTaskScheduler,
) : ModelDownloadRuntime {

    override suspend fun enqueueModelDownload(group: ModelGroup) {
        scheduler.downloadModels(group)
    }

    override fun cancelModelDownload(group: ModelGroup) {
        scheduler.cancelModelDownload(group)
    }
}
