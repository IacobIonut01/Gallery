/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.ui

import android.provider.Settings.Global
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the system animator-duration scale is 0 (accessibility
 * reduce-motion). Read once per composition — same semantics the pre-port
 * `rememberPreviewAnimation` had.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Global.getFloat(
            context.contentResolver,
            Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
