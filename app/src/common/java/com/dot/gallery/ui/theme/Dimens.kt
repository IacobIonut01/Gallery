/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed class Dimens(val size: Dp) {
    object Photo : Dimens(size = 100.dp)
    object Album : Dimens(size = 182.dp)

    operator fun invoke(): Dp = size
}