/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import androidx.compose.runtime.compositionLocalOf

val LocalMediaSelector = compositionLocalOf<MediaSelector> {
    error("No MediaSelector provided!!! This is likely due to a missing inject binding in the Composable hierarchy.")
}
