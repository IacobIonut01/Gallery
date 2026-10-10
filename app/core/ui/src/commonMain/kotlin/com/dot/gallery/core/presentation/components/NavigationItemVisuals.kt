/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.font.FontWeight
import com.dot.gallery.core.presentation.util.NavigationItem

/** Shared label/icon visuals for the classic bar and rail. */
@Stable
@Composable
internal fun NavigationItemLabel(item: NavigationItem) = Text(
    text = item.name,
    fontWeight = FontWeight.Medium,
    style = MaterialTheme.typography.bodyMedium,
)

@Stable
@Composable
internal fun NavigationItemIcon(item: NavigationItem) = Icon(
    imageVector = item.icon,
    contentDescription = item.name,
)
