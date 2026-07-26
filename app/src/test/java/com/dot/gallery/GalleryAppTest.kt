package com.dot.gallery

import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class GalleryAppTest {

    @Test
    fun sketchCacheDirectoryUsesInternalCacheDirectory() {
        val internalCacheDirectory = File("/data/user/0/com.dot.gallery/cache")

        val result = sketchCacheDirectory(internalCacheDirectory)

        assertEquals(internalCacheDirectory.absolutePath.toPath(), result.parent)
        assertEquals("sketch", result.name)
    }
}
