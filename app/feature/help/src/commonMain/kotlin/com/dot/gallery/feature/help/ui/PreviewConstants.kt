/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.ui

import androidx.compose.animation.core.EaseInOutCubic

/** Timing defaults shared by the help preview state machines. */
object PreviewConstants {
    const val DEFAULT_STEP_DURATION_MS = 1200L
    const val DEFAULT_PAUSE_DURATION_MS = 1500L
    val DEFAULT_EASING = EaseInOutCubic
}
