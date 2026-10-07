/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Stable

@Stable
data class SettingsCloudIndexingState(
    val providerSmartSearch: Boolean = false,
    val providers: List<CloudIndexingProviderState> = emptyList(),
)
