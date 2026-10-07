/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.cloud.core.ProviderType

/**
 * Per-provider row state for [SettingsCloudIndexingContent]. The wrapper
 * pre-computes [delegatesSmartSearch] so the content composable never touches
 * `ProviderCapability`.
 */
@Stable
data class CloudIndexingProviderState(
    val type: ProviderType,
    val indexOnDevice: Boolean,
    val delegatesSmartSearch: Boolean,
)
