/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.data.entity.SmartScanRunEntity

@Stable
data class CategoriesSettingsState(
    val categoryScan: SmartScanRunEntity? = null,
    val workerProgress: Float = 0f,
    val hasCategories: Boolean = false,
    val modelsReady: Boolean = false,
    /** Non-null while the "purge existing video entries" dialog is shown. */
    val videoPurgeCount: Int? = null,
)
