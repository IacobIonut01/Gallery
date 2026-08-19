/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import androidx.compose.ui.unit.LayoutDirection
import com.dot.gallery.core.presentation.components.navigationRailSlideOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationRailAnimationTest {

    @Test
    fun railSlidesFromLogicalStartEdge() {
        assertEquals(-160, navigationRailSlideOffset(80, LayoutDirection.Ltr))
        assertEquals(160, navigationRailSlideOffset(80, LayoutDirection.Rtl))
    }
}
