/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.domain.settings.SettingsKeys

@Stable
class FeedbackManager(
    private val hapticFeedback: HapticFeedback,
    private val allowVibrations: Boolean,
) {

    fun vibrate() {
        if (allowVibrations) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
        }
    }

    fun vibrateStrong() {
        if (allowVibrations) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberFeedbackManager(): FeedbackManager {
    val hapticFeedback = LocalHapticFeedback.current
    val allowVibrations by rememberPreference(SettingsKeys.allowVibrations)
    return remember(hapticFeedback, allowVibrations) {
        FeedbackManager(hapticFeedback, allowVibrations)
    }
}
