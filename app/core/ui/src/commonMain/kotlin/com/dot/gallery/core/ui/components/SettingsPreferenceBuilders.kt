/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity

@Composable
fun rememberSwitchPreference(
    vararg keys: Any? = emptyArray(),
    title: String,
    icon: ImageVector? = null,
    summary: String? = null,
    isChecked: Boolean,
    onCheck: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    screenPosition: Position
) = remember(
    *keys,
    title,
    summary,
    isChecked,
    onCheck,
    onClick,
    enabled,
    screenPosition
) {
    SettingsEntity.SwitchPreference(
        title = title,
        summary = summary,
        icon = icon,
        isChecked = isChecked,
        onCheck = onCheck,
        onClick = onClick,
        enabled = enabled,
        screenPosition = screenPosition
    )
}

@Composable
fun rememberPreference(
    vararg keys: Any? = emptyArray(),
    title: String,
    icon: ImageVector? = null,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    screenPosition: Position
) = remember(
    *keys,
    title,
    summary,
    enabled,
    onClick,
    screenPosition
) {
    SettingsEntity.Preference(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        screenPosition = screenPosition
    )
}
