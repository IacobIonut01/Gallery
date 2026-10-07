/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.ml.ModelStatus


/**
 * Everything [LibraryContent] needs to render — the persisted [snapshot] plus
 * platform-resolved facts the caller measured app-side (insets visibility comes
 * from ViewCompat, the bottom inset from the navigation pill geometry).
 */
@Immutable
data class LibraryState(
    val snapshot: LibrarySnapshot,
    val modelStatus: ModelStatus,
    val aiAvailable: Boolean,
    val mapsEnabled: Boolean,
    val statusBarTopPx: Int = 0,
    val statusBarsVisible: Boolean? = null,
    val bottomBarInset: Dp = 0.dp,
    val navigationBarsVisible: Boolean? = null,
    /** Persisted album-grid cell index (SharedPreferences-backed app-side). */
    val cellsIndex: Int = 0,
    /** `SdkCompat.supportsTrash` — platform capability, measured app-side. */
    val supportsTrash: Boolean = true,
    /** `SdkCompat.supportsFavorites` — platform capability, measured app-side. */
    val supportsFavorites: Boolean = true,
)
