package com.dot.gallery.frameextract

import com.dot.gallery.feature_node.presentation.frameextract.VideoLocationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoLocationParserTest {
    @Test
    fun parsesIso6709Coordinates() {
        assertEquals(47.1234 to -122.9876, VideoLocationParser.parse("+47.1234-122.9876/"))
        assertEquals(-33.9 to 151.2, VideoLocationParser.parse("-33.9+151.2+003.0/"))
    }

    @Test
    fun rejectsMalformedOrOutOfRangeCoordinates() {
        assertNull(VideoLocationParser.parse(null))
        assertNull(VideoLocationParser.parse("not-a-location"))
        assertNull(VideoLocationParser.parse("+91.0+010.0/"))
        assertNull(VideoLocationParser.parse("+10.0+181.0/"))
    }
}
