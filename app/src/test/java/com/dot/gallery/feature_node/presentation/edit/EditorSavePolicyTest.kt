package com.dot.gallery.feature_node.presentation.edit

import com.dot.gallery.core.decoder.format.ImageReencoder
import com.dot.gallery.feature_node.presentation.edit.components.develop.RawSaveFormat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorSavePolicyTest {
    @Test
    fun forcePngUsesPngNameEvenForJpegSource() {
        val output = EditorOutputPolicy.copy("image/jpeg", "photo.jpeg", forcePng = true)

        assertEquals(ImageReencoder.ImageWriteFormat.PNG, output.writeFormat)
        assertEquals("photo.png", output.displayName)
    }

    @Test
    fun nonEncodableSourceFallsBackToPngName() {
        val output = EditorOutputPolicy.copy("image/x-adobe-dng", "capture.dng", forcePng = false)

        assertEquals(ImageReencoder.ImageWriteFormat.PNG, output.writeFormat)
        assertEquals("capture.png", output.displayName)
    }

    @Test
    fun rawNamesMatchSelectedJpegPngAndTiffFormats() {
        assertEquals("capture_developed.jpg", EditorOutputPolicy.rawCopy("capture.dng", RawSaveFormat.JPEG))
        assertEquals("capture_developed.png", EditorOutputPolicy.rawCopy("capture.dng", RawSaveFormat.PNG))
        assertEquals("capture_developed.tiff", EditorOutputPolicy.rawCopy("capture.dng", RawSaveFormat.TIFF_16))
    }

    @Test
    fun saveGuardRejectsOverlapAndReleasesAfterCompletion() = runBlocking {
        val guard = EditorSaveGuard()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val first = async {
            guard.runIfIdle {
                entered.complete(Unit)
                release.await()
            }
        }
        entered.await()

        assertFalse(guard.runIfIdle { error("overlapping save must not run") })
        release.complete(Unit)
        assertTrue(first.await())
        assertTrue(guard.runIfIdle {})
    }

    @Test
    fun saveGuardReleasesWhenOperationThrows() = runBlocking {
        val guard = EditorSaveGuard()

        runCatching { guard.runIfIdle { error("encode failed") } }

        assertTrue(guard.runIfIdle {})
    }
}
