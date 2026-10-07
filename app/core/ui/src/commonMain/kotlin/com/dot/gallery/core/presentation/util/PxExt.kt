/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Stable
fun Int.roundDpToPx(density: Density) = with(density) { dp.roundToPx() }

@Stable
fun Int.roundSpToPx(density: Density) = with(density) { sp.roundToPx() }
