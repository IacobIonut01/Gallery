package com.dot.gallery.metadata

import com.dot.gallery.core.metadata.MediaContainerFormat
import com.dot.gallery.core.metadata.MetadataCapabilities
import com.dot.gallery.core.metadata.MetadataRemovalMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataCapabilitiesTest {
    @Test
    fun commonRasterModesAreAvailable() {
        MetadataRemovalMode.entries.forEach { mode ->
            assertTrue(MetadataCapabilities.forFormat(MediaContainerFormat.JPEG).supports(mode))
            assertTrue(MetadataCapabilities.forFormat(MediaContainerFormat.PNG).supports(mode))
            assertTrue(MetadataCapabilities.forFormat(MediaContainerFormat.WEBP).supports(mode))
        }
        assertFalse(MetadataCapabilities.forFormat(MediaContainerFormat.GIF).supports(MetadataRemovalMode.LOCATION))
        assertTrue(MetadataCapabilities.forFormat(MediaContainerFormat.GIF).supports(MetadataRemovalMode.PRIVACY))
        assertFalse(MetadataCapabilities.forFormat(MediaContainerFormat.BMP).supports(MetadataRemovalMode.EVERYTHING))
        assertTrue(MetadataCapabilities.forFormat(MediaContainerFormat.JP2).supports(MetadataRemovalMode.EVERYTHING))
        assertFalse(MetadataCapabilities.forFormat(MediaContainerFormat.JP2).supports(MetadataRemovalMode.PRIVACY))
        assertTrue(MetadataCapabilities.forFormat(MediaContainerFormat.JXL).supports(MetadataRemovalMode.EVERYTHING))
    }

    @Test
    fun unfinishedWritersFailClosed() {
        assertFalse(MetadataCapabilities.forFormat(MediaContainerFormat.TIFF).supports(MetadataRemovalMode.EVERYTHING))
        assertFalse(MetadataCapabilities.forFormat(MediaContainerFormat.MP4).supports(MetadataRemovalMode.LOCATION))
        assertFalse(MetadataCapabilities.forFormat(MediaContainerFormat.UNKNOWN).supports(MetadataRemovalMode.PRIVACY))
    }
}
