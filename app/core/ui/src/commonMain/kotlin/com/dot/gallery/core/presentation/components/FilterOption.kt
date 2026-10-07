/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import com.dot.gallery.domain.util.MediaOrder
import org.jetbrains.compose.resources.StringResource

data class FilterOption(
    val titleRes: StringResource,
    val onClick: (MediaOrder) -> Unit = {},
    val filterKind: FilterKind = FilterKind.DATE
)
