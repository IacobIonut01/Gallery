/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif

import com.dot.gallery.domain.model.Media
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Decoded snapshot of a `MediaCopyWorker` work item. Null fields mean the
 * worker has not reported them yet (progress keys ride on `WorkInfo.progress`
 * while running and `outputData` once finished).
 */
data class LocalCopySnapshot(
    val state: CopyWorkState,
    val progressPercent: Int? = null,
    val targetUri: String? = null,
)