/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.feature.help.domain.HelpRoute

@Stable
class HelpActions(
    /** Navigate to a help-flow destination — the wrapper resolves it to `Screen.*`. */
    val onNavigate: (HelpRoute) -> Unit = {},
)
