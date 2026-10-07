/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import com.dot.gallery.core.ml.DownloadInfo
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelStatus

/** Per-group observable bundle, mirrors the ModelManager flows. */
data class AiModelGroupUi(
    val group: ModelGroup,
    val status: ModelStatus = ModelStatus.NOT_INSTALLED,
    val progress: Float = 0f,
    val info: DownloadInfo = DownloadInfo(),
    val error: String? = null,
    val installedSize: Long = 0L,
)
