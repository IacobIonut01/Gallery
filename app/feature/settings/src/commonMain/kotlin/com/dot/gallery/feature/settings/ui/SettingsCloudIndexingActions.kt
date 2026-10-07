/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.cloud.core.ProviderType

@Stable
class SettingsCloudIndexingActions(
    val onProviderSmartSearchChange: (Boolean) -> Unit = {},
    val onIndexOnDeviceChange: (ProviderType, Boolean) -> Unit = { _, _ -> },
)
