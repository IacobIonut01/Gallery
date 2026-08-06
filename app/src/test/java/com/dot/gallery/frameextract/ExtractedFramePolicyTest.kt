package com.dot.gallery.frameextract

import com.dot.gallery.feature_node.presentation.frameextract.ExtractedFrameDestination
import com.dot.gallery.feature_node.presentation.frameextract.ExtractedFrameName
import com.dot.gallery.feature_node.presentation.frameextract.FrameExportFormat
import com.dot.gallery.feature_node.presentation.frameextract.FrameIdentity
import com.dot.gallery.feature_node.presentation.frameextract.FrameSourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractedFramePolicyTest {
    @Test
    fun onlyEligibleLocalImageRootsArePreserved() {
        assertEquals("DCIM/Camera", ExtractedFrameDestination.resolve(FrameSourceKind.LOCAL, "DCIM/Camera/"))
        assertEquals("Pictures/Trips", ExtractedFrameDestination.resolve(FrameSourceKind.LOCAL, "/Pictures/Trips"))
        assertEquals(ExtractedFrameDestination.FALLBACK, ExtractedFrameDestination.resolve(FrameSourceKind.LOCAL, "Movies/Clips"))
        assertEquals(ExtractedFrameDestination.FALLBACK, ExtractedFrameDestination.resolve(FrameSourceKind.LOCAL, "Pictures/../Secret"))
        assertEquals(ExtractedFrameDestination.FALLBACK, ExtractedFrameDestination.resolve(FrameSourceKind.CLOUD, "DCIM/Camera"))
        assertEquals(ExtractedFrameDestination.FALLBACK, ExtractedFrameDestination.resolve(FrameSourceKind.VAULT, "Pictures"))
    }

    @Test
    fun namesAreSanitizedAndFormatSpecific() {
        val jpeg = ExtractedFrameName.create(
            "bad:name?.mp4",
            FrameIdentity(24, 3_723_456_000),
            FrameExportFormat.JPEG,
            "abc123",
        )
        val png = ExtractedFrameName.create(
            "clip.mov",
            FrameIdentity(-1, 0),
            FrameExportFormat.PNG,
            "token",
        )

        assertFalse(jpeg.contains(':'))
        assertTrue(jpeg.endsWith(".jpg"))
        assertTrue(jpeg.contains("01-02-03-456_24"))
        assertTrue(png.endsWith(".png"))
        assertTrue(png.contains("_time_"))
    }
}
