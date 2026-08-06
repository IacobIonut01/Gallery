package com.dot.gallery.frameextract

import com.dot.gallery.feature_node.presentation.frameextract.FrameExportResultAggregator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameExportResultAggregatorTest {
    @Test
    fun reportsSuccessAndPartialFailureWithoutDroppingSavedUris() {
        val success = FrameExportResultAggregator.aggregate(
            savedValues = listOf("content://media/image/1"),
            failed = 0,
            cancelled = false,
            warnings = 1,
        )
        val partial = FrameExportResultAggregator.aggregate(
            savedValues = listOf("content://media/image/1", "content://media/image/2"),
            failed = 1,
            cancelled = false,
            warnings = 0,
        )

        assertFalse(success.isPartial)
        assertEquals(1, success.warnings)
        assertTrue(partial.isPartial)
        assertEquals(2, partial.saved.size)
    }

    @Test
    fun preservesCancellationState() {
        val result = FrameExportResultAggregator.aggregate(emptyList(), 0, true, 0)
        assertTrue(result.cancelled)
    }
}
