/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.scrollbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollbarGeometryTest {

    private val viewport = 2_000f

    /**
     * The reported bug: scrolling the timeline made the thumb jerk, skip and drift back.
     *
     * The position used to be derived from an item index, and a mosaic lays several items in one
     * row — the index therefore jumped by two to four while the content moved one row, so equal
     * finger movement produced a crawl followed by a leap. Pixel movement must produce equal
     * thumb movement, whatever the index does.
     */
    @Test
    fun `equal pixel movement advances the thumb equally even when the item index jumps`() {
        val tracker = GridScrollPixelTracker()
        val content = 20_000f
        val rowHeight = 100
        val itemsPerRow = 3

        // Items of one row share an offset; the row above this one is off screen. Every third
        // step a whole row leaves the window, so the reported first visible index jumps by three.
        fun frameAt(scroll: Int): List<GridScrollItem> =
            (0 until 60).mapNotNull { index ->
                val offset = (index / itemsPerRow) * rowHeight - scroll
                if (offset <= -rowHeight || offset >= viewport.toInt()) {
                    null
                } else {
                    GridScrollItem(index = index, offsetY = offset, height = rowHeight)
                }
            }

        val progress = ArrayList<Float>()
        for (step in 0..40) {
            // The first pass has nothing to carry over and must be seeded; every later pass must
            // take the position from the carried-over item, so a deliberately wrong seed is
            // ignored there.
            val seed = if (step == 0) 0f else -1f
            val pixels = tracker.update(
                items = frameAt(step * 10),
                viewportPixels = viewport.toInt(),
                seedPixels = seed,
                indexIncrementPixels = 10f,
            )
            assertEquals(step * 10f, pixels, 1e-3f)
            progress += scrollbarProgress(pixels, content, viewport)
        }

        val steps = progress.zipWithNext { previous, next -> next - previous }
        val expected = 10f / (content - viewport)
        steps.forEach { step ->
            assertEquals(expected, step, 1e-6f)
        }
        assertTrue(progress.first() < progress.last())
    }

    @Test
    fun `the tracker seeds once and advances when nothing carries over`() {
        val tracker = GridScrollPixelTracker()

        assertEquals(
            0f,
            tracker.update(
                items = listOf(item(0, 0), item(1, 0)),
                viewportPixels = viewport.toInt(),
                seedPixels = 0f,
                indexIncrementPixels = 0f,
            ),
            1e-3f,
        )
        // A jump: nothing that was on screen is on screen any more.
        assertEquals(
            7_500f,
            tracker.update(
                items = listOf(item(50, -10), item(51, -10)),
                viewportPixels = viewport.toInt(),
                seedPixels = 7_500f,
                indexIncrementPixels = 7_500f,
            ),
            1e-3f,
        )
        assertEquals(
            7_600f,
            tracker.update(
                items = listOf(item(50, -110), item(51, -110)),
                viewportPixels = viewport.toInt(),
                seedPixels = 0f,
                indexIncrementPixels = 100f,
            ),
            1e-3f,
        )
    }

    /**
     * A sticky header is pinned to the top edge and stays there while the content scrolls under
     * it. Anchoring the integration on it measured no movement at all, which lost about half of
     * the scroll distance — the position then had to be corrected in one visible jump.
     */
    @Test
    fun `a pinned sticky header does not carry the position`() {
        val tracker = GridScrollPixelTracker()

        assertEquals(
            0f,
            tracker.update(
                items = listOf(item(5, 0), item(6, 100), item(7, 400)),
                viewportPixels = viewport.toInt(),
                seedPixels = 0f,
                indexIncrementPixels = 0f,
            ),
            1e-3f,
        )
        // The content moved 100 px: the pinned header did not move, the tiles did.
        assertEquals(
            100f,
            tracker.update(
                items = listOf(item(5, 0), item(6, 0), item(7, 300)),
                viewportPixels = viewport.toInt(),
                seedPixels = 999f,
                indexIncrementPixels = 0f,
            ),
            1e-3f,
        )
    }

    @Test
    fun `pixels and thumb position are inverses of each other`() {
        val scrollable = 18_000f
        val displayed = 0.1f

        assertEquals(0f, scrollbarThumbTop(progress = 0f, displayedShare = displayed), 1e-6f)
        assertEquals(
            0.9f,
            scrollbarThumbTop(progress = 1f, displayedShare = displayed),
            1e-6f,
        )
        assertEquals(
            scrollable,
            scrollbarPixelsAtThumbTop(
                thumbTop = scrollbarThumbTop(1f, displayed),
                displayedShare = displayed,
                scrollablePixels = scrollable,
            ),
            1e-3f,
        )
        assertEquals(
            scrollable / 2f,
            scrollbarPixelsAtThumbTop(
                thumbTop = scrollbarThumbTop(0.5f, displayed),
                displayedShare = displayed,
                scrollablePixels = scrollable,
            ),
            1e-3f,
        )
        // A thumb that fills the track cannot travel, so a drag on it points at the start.
        assertEquals(
            0f,
            scrollbarPixelsAtThumbTop(thumbTop = 0.5f, displayedShare = 1f, scrollablePixels = scrollable),
            1e-6f,
        )
    }

    @Test
    fun `progress spans the scrollable range and clamps at both ends`() {
        val content = 20_000f

        assertEquals(0f, scrollbarProgress(0f, content, viewport), 1e-6f)
        assertEquals(1f, scrollbarProgress(content - viewport, content, viewport), 1e-6f)
        assertEquals(0.5f, scrollbarProgress((content - viewport) / 2f, content, viewport), 1e-6f)
        assertEquals(0f, scrollbarProgress(-500f, content, viewport), 1e-6f)
        assertEquals(1f, scrollbarProgress(content * 2f, content, viewport), 1e-6f)
        // Content shorter than the viewport is not scrollable at all.
        assertEquals(0f, scrollbarProgress(0f, contentPixels = 100f, visiblePixels = viewport), 1e-6f)
        assertEquals(1f, scrollbarProgress(50f, contentPixels = 100f, visiblePixels = viewport), 1e-6f)
    }

    @Test
    fun `content height comes from the distance the content moves per item`() {
        // Items share rows, so the content is far shorter than the tile heights stacked: the scale
        // that matters is how far the list travels per item passed.
        assertEquals(
            20_000f,
            scrollbarContentPixels(pixelsPerItem = 100f, totalItems = 200),
            1e-6f,
        )
        assertEquals(0f, scrollbarContentPixels(pixelsPerItem = 0f, totalItems = 200), 1e-6f)
        assertEquals(0f, scrollbarContentPixels(pixelsPerItem = 100f, totalItems = 0), 1e-6f)

        assertEquals(0.1f, scrollbarVisibleShare(viewport, 20_000f), 1e-6f)
        // A library so large that one viewport is less than a percent of it still reports a share
        // it can draw (the caller clamps it to the minimum thumb length).
        assertEquals(0.001f, scrollbarVisibleShare(viewport, 2_000_000f), 1e-6f)
        assertEquals(1f, scrollbarVisibleShare(viewport, 500f), 1e-6f)
        assertEquals(0f, scrollbarVisibleShare(viewport, 0f), 1e-6f)
    }

    @Test
    fun `items above an item is its index plus the fraction scrolled into it`() {
        assertEquals(3f, scrollbarItemsAbove(3, -0.5f), 1e-6f)
        assertEquals(4f, scrollbarItemsAbove(3, 1.5f), 1e-6f)
        assertEquals(3.25f, scrollbarItemsAbove(3, 0.25f), 1e-6f)
    }

    private fun item(index: Int, offsetY: Int) = GridScrollItem(index, offsetY, height = 100)
}
