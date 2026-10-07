/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.subsettings

import com.dot.gallery.core.ml.ModelGroup

/**
 * App-provided backend for model download work — WorkManager stays app-side while
 * the settings ViewModels live in this module.
 */
interface ModelDownloadRuntime {

    /** Enqueues a model-download work item for [group]. */
    suspend fun enqueueModelDownload(group: ModelGroup)

    /** Cancels any queued/in-flight download for [group]. */
    fun cancelModelDownload(group: ModelGroup)
}
