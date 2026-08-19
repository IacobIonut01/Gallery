/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
sealed class Dimens(val size: Dp) {
    data object Photo : Dimens(size = 100.dp)
    data object Album : Dimens(size = 178.dp)

    operator fun invoke(): Dp = size
}

/** Shared spacing roles for reusable UI rather than one-off numeric values. */
object Spacing {
    val Hairline = 1.dp
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val ScreenHorizontal = 16.dp
    val ContentHorizontal = 24.dp
}

/** Shared component sizes with accessibility-sensitive minimums. */
object ComponentSize {
    val MinimumTouchTarget = 48.dp
    val ButtonHeight = 64.dp
    val NavigationBarHeight = 64.dp
    val NavigationRailWidth = 80.dp
    val StateIcon = 96.dp
}