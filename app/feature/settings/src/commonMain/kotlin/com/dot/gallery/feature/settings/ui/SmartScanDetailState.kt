/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.data.entity.SmartScanPhaseEntity
import com.dot.gallery.data.entity.SmartScanRunEntity

@Stable
data class SmartScanDetailState(
    val activeScan: SmartScanRunEntity? = null,
    val activePhases: List<SmartScanPhaseEntity> = emptyList(),
    val latestScan: SmartScanRunEntity? = null,
    val searchReady: Boolean = false,
    val personsReady: Boolean = false,
)
