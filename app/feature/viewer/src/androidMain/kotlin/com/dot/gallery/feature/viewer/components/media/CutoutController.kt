/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer.components.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dot.gallery.core.util.or
import com.dot.gallery.core.ml.CutoutHelper.CutoutResult
import com.dot.gallery.core.ml.CutoutHelper.CutoutSession
import com.dot.gallery.core.ml.CutoutHelper.PromptPoint

/**
 * Presentation-facing bundle the media viewer publishes upward so the cutout controls can live in
 * the [MediaViewScreen] bottom bar (replacing the quick actions) instead of floating over the image.
 * All session/coroutine work stays in [ZoomablePagerImage]; the screen only invokes these lambdas.
 */
@Stable
class CutoutController(
    val state: CutoutState,
    val onToolChange: (ZoomablePagerImagePointTool) -> Unit,
    val onReset: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onCopy: () -> Unit,
    val onShare: () -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
)