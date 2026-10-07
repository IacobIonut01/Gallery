/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier

/**
 * Scope provided by [GridPinchZoomLayout] to its content.
 * Drop-in replacement for the library's PinchZoomGridScope.
 */
interface GridPinchZoomScope {
    val gridState: LazyGridState
    val gridCells: GridCells

    /**
     * No-op modifier kept for source compatibility with items
     * that previously used the library's pinchItem transitions.
     */
    fun Modifier.pinchItem(key: Any): Modifier = this
}
