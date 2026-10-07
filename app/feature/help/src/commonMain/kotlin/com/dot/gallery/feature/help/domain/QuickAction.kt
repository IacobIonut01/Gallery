/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.domain

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

/**
 * A live-navigation shortcut shown on a [HelpTip] detail screen and in the
 * unified search results. Always navigates to a real screen route; it never
 * mutates state from the help context.
 */
@Immutable
data class QuickAction(
    val label: StringResource,
    val icon: ImageVector,
    val route: HelpRoute
)
