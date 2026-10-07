/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Mutable holder driven by [rememberPreviewAnimation]. */
@Stable
class PreviewAnimationState(val stepCount: Int) {
    var currentStep by mutableIntStateOf(0)
        internal set
    var stepProgress by mutableFloatStateOf(0f)
        internal set
}
