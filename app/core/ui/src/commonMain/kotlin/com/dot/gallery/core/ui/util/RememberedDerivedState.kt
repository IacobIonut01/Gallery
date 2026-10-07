/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
fun <T> rememberedDerivedState(
    key: Any? = Unit,
    block: @DisallowComposableCalls () -> T
): State<T> {
    return remember(key) {
        derivedStateOf(block)
    }
}

@Composable
fun <T> rememberedDerivedState(
    vararg keys: Any? = arrayOf(Unit),
    block: @DisallowComposableCalls () -> T
): State<T> {
    return remember(*keys) {
        derivedStateOf(block)
    }
}
