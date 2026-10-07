/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

import com.dot.gallery.core.ml.ModelStatus

/** Per-model-group display state on the AI models page. */
data class SetupModelUi(
    val status: ModelStatus = ModelStatus.NOT_INSTALLED,
    /** Download progress in percent (0–100), as reported by the model manager. */
    val progress: Float = 0f,
)
