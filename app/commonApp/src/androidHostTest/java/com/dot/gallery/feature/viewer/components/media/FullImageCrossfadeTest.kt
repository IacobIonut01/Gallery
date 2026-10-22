package com.dot.gallery.feature.viewer.components.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FullImageCrossfadeTest {

    @Test
    fun itemAnimationOffDisablesTheCrossfade() {
        assertNull(fullImageCrossfadeMillis(animationsEnabled = false))
    }

    @Test
    fun itemAnimationOnCrossfadesForTwoHundredMillis() {
        assertEquals(200, fullImageCrossfadeMillis(animationsEnabled = true))
    }
}
