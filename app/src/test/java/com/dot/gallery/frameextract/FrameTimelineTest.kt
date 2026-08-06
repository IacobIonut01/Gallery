package com.dot.gallery.frameextract

import com.dot.gallery.feature_node.presentation.frameextract.FrameIdentity
import com.dot.gallery.feature_node.presentation.frameextract.FrameTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameTimelineTest {
    @Test
    fun constantFrameRateConvertsAndClampsIndexes() {
        val timeline = FrameTimeline.constant(2_000_000L, 60, 30f)

        assertTrue(timeline.usesFrameIndexes)
        assertEquals(FrameIdentity(0, 0), timeline.closest(-100))
        assertEquals(30, timeline.closest(1_000_000).frameIndex)
        assertEquals(59, timeline.closest(Long.MAX_VALUE).frameIndex)
        assertEquals(1, timeline.step(timeline.first, 1).frameIndex)
        assertEquals(0, timeline.step(timeline.first, -1).frameIndex)
    }

    @Test
    fun variableFrameRateUsesRealNeighborTimestamps() {
        val timeline = FrameTimeline.variable(
            durationUs = 1_000_000,
            timestampsUs = listOf(0, 33_000, 70_000, 70_000, 125_000, 500_000),
        )

        assertFalse(timeline.usesFrameIndexes)
        assertEquals(70_000L, timeline.closest(80_000).presentationTimeUs)
        assertEquals(125_000L, timeline.step(FrameIdentity(2, 70_000), 1).presentationTimeUs)
        assertEquals(33_000L, timeline.step(FrameIdentity(2, 70_000), -1).presentationTimeUs)
    }

    @Test
    fun unknownRateFallsBackWithoutInventingAnIndex() {
        val timeline = FrameTimeline.unknown(1_000_000, null)
        val stepped = timeline.step(FrameIdentity(-1, 100_000), 1)

        assertEquals(-1, stepped.frameIndex)
        assertEquals(133_333L, stepped.presentationTimeUs)
    }
}
