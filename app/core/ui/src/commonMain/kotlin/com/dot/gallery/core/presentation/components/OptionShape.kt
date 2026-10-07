/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp


object OptionShape {

    val Top = RoundedCornerShape(
        topEnd = 24.dp,
        topStart = 24.dp,
        bottomEnd = 2.dp,
        bottomStart = 2.dp
    )

    val Middle = RoundedCornerShape(
        topEnd = 2.dp,
        topStart = 2.dp,
        bottomEnd = 2.dp,
        bottomStart = 2.dp
    )

    val Bottom = RoundedCornerShape(
        topEnd = 2.dp,
        topStart = 2.dp,
        bottomEnd = 24.dp,
        bottomStart = 24.dp
    )

    val Alone = RoundedCornerShape(
        topEnd = 24.dp,
        topStart = 24.dp,
        bottomEnd = 24.dp,
        bottomStart = 24.dp
    )
}
