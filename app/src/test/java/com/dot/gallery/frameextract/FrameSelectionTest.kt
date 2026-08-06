package com.dot.gallery.frameextract

import com.dot.gallery.feature_node.presentation.frameextract.FrameIdentity
import com.dot.gallery.feature_node.presentation.frameextract.FrameSelectionReducer
import com.dot.gallery.feature_node.presentation.frameextract.SelectionChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameSelectionTest {
    @Test
    fun toggleOrdersFramesAndPreventsDuplicates() {
        val later = FrameIdentity(2, 200)
        val earlier = FrameIdentity(1, 100)
        val first = FrameSelectionReducer.toggle(emptyList(), later) as SelectionChange.Updated
        val second = FrameSelectionReducer.toggle(first.frames, earlier) as SelectionChange.Updated

        assertEquals(listOf(earlier, later), second.frames)
        val removed = FrameSelectionReducer.toggle(second.frames, earlier) as SelectionChange.Updated
        assertEquals(listOf(later), removed.frames)
    }

    @Test
    fun selectionStopsAtFiftyFrames() {
        val selection = List(FrameSelectionReducer.MAX_SELECTION) { FrameIdentity(it, it.toLong()) }
        assertTrue(
            FrameSelectionReducer.toggle(selection, FrameIdentity(51, 51)) is SelectionChange.LimitReached
        )
    }

    @Test
    fun restoreDropsMalformedDuplicatesAndOverflow() {
        val values = buildList {
            add("bad")
            repeat(60) { add("$it:$it") }
            add("1:1")
        }
        val restored = FrameSelectionReducer.restore(values)

        assertEquals(50, restored.size)
        assertEquals(restored.distinct(), restored)
    }
}
