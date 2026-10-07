package com.dot.gallery.app.presentation.common.components

import com.dot.gallery.core.presentation.components.safeMosaicColumns
import com.dot.gallery.core.presentation.components.timelineScrollbarSettings
import com.dot.gallery.core.presentation.components.safeMosaicSpan
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import com.dot.gallery.core.ui.components.settings
import com.dot.gallery.scrollbar.ScrollbarSelectionActionable



class MosaicGridBoundsTest {

    @Test
    fun `columns are clamped to supported bounds`() {
        assertEquals(3, safeMosaicColumns(Int.MIN_VALUE))
        assertEquals(3, safeMosaicColumns(0))
        assertEquals(3, safeMosaicColumns(3))
        assertEquals(4, safeMosaicColumns(4))
        assertEquals(5, safeMosaicColumns(5))
        assertEquals(6, safeMosaicColumns(6))
        assertEquals(6, safeMosaicColumns(Int.MAX_VALUE))
    }

    @Test
    fun `timeline scrollbar remains available after scrolling stops`() {
        val settings = timelineScrollbarSettings(enabled = true)

        assertEquals(2_000, settings.hideDelayMillis)
        assertEquals(24.dp, settings.indicatorOffset)
        assertEquals(48.dp, settings.hideDisplacement)
        assertEquals(ScrollbarSelectionActionable.WhenVisible, settings.selectionActionable)
    }

    @Test
    fun `spans are always valid for effective column count`() {
        assertEquals(1, safeMosaicSpan(Int.MIN_VALUE, Int.MIN_VALUE))
        assertEquals(1, safeMosaicSpan(0, 4))
        assertEquals(3, safeMosaicSpan(Int.MAX_VALUE, 3))
        assertEquals(4, safeMosaicSpan(4, 4))
        assertEquals(6, safeMosaicSpan(Int.MAX_VALUE, Int.MAX_VALUE))
    }
}
