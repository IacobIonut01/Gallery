package com.dot.gallery.frameextract

import com.dot.gallery.feature_node.domain.util.MotionPhotoHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPhotoHelperTest {
    @Test
    fun explicitMotionPhotoOffsetWins() {
        val info = MotionPhotoHelper.resolveInfo(
            mapOf(
                "GCamera:MotionPhoto" to "1",
                "GCamera:MotionPhotoVideoOffset" to "2048",
                "GCamera:MicroVideoOffset" to "1024",
                "GCamera:MotionPhotoPresentationTimestampUs" to "500000",
            )
        )

        assertEquals(2048L, info?.videoOffset)
        assertEquals(500000L, info?.presentationTimestampUs)
    }

    @Test
    fun containerLengthAndPaddingAreValidated() {
        val info = MotionPhotoHelper.resolveInfo(
            mapOf(
                "GCamera:MotionPhoto" to "1",
                "Container:Directory[2]/Item:Semantic" to "MotionPhoto",
                "Container:Directory[2]/Item:Length" to "1000",
                "Container:Directory[2]/Item:Padding" to "24",
            )
        )

        assertEquals(1024L, info?.videoOffset)
    }

    @Test
    fun microVideoFallbackIsSupported() {
        val info = MotionPhotoHelper.resolveInfo(
            mapOf(
                "GCamera:MicroVideo" to "1",
                "GCamera:MicroVideoOffset" to "512",
                "GCamera:MicroVideoPresentationTimestampUs" to "123",
            )
        )

        assertEquals(512L, info?.videoOffset)
        assertEquals(123L, info?.presentationTimestampUs)
    }

    @Test
    fun malformedAndOverflowOffsetsAreRejected() {
        assertNull(
            MotionPhotoHelper.resolveInfo(
                mapOf(
                    "GCamera:MotionPhoto" to "1",
                    "Container:Directory[1]/Item:Semantic" to "MotionPhoto",
                    "Container:Directory[1]/Item:Length" to Long.MAX_VALUE.toString(),
                    "Container:Directory[1]/Item:Padding" to "1",
                )
            )
        )
        assertNull(MotionPhotoHelper.videoStart(100, 101))
        assertNull(MotionPhotoHelper.videoStart(100, 0))
    }

    @Test
    fun samsungMarkerResolvesOffsetFromEnd() {
        val marker = "MotionPhoto_Data".toByteArray(Charsets.US_ASCII)
        val video = ByteArray(20)
        val bytes = ByteArray(12) + marker + video

        assertEquals(video.size.toLong(), MotionPhotoHelper.findSamsungMarkerOffset(bytes))
    }

    @Test
    fun ftypValidationRequiresTheBoxSignature() {
        val valid = byteArrayOf(0, 0, 0, 24, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte())
        assertTrue(MotionPhotoHelper.hasMp4Ftyp(valid))
        assertEquals(60L, MotionPhotoHelper.videoStart(100, 40))
    }
}
