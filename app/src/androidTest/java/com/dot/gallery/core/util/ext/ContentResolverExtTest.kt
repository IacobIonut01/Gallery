package com.dot.gallery.core.util.ext

import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ContentResolverExtTest {

    @Test
    fun streamedOverrideKeepsSourceReadableUntilStagingCompletes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val original = "original-image-bytes".encodeToByteArray()
        val replacement = "edited-image-bytes".encodeToByteArray()
        val uri = resolver.saveImageStreaming(
            mimeType = "image/png",
            relativePath = Environment.DIRECTORY_PICTURES + "/Edited",
            displayName = "override-${System.nanoTime()}.png",
        ) { fd ->
            writeBytes(fd, original)
        }
        assertNotNull(uri)
        val target = requireNotNull(uri)

        val stagingFile = File.createTempFile("override-test-", ".tmp", context.cacheDir)
        try {
            var sourceDuringWrite: ByteArray? = null
            val success = resolver.overrideImageStreaming(target, stagingFile) { fd ->
                sourceDuringWrite = resolver.openInputStream(target)?.use { it.readBytes() }
                writeBytes(fd, replacement)
            }

            assertTrue(success)
            assertArrayEquals(original, sourceDuringWrite)
            assertArrayEquals(replacement, resolver.openInputStream(target)?.use { it.readBytes() })
            assertFalse(stagingFile.exists())
        } finally {
            resolver.delete(target, null, null)
            stagingFile.delete()
        }
    }

    @Test
    fun failedStreamLeavesSourceUnchanged() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val original = "original-image-bytes".encodeToByteArray()
        val uri = resolver.saveImageStreaming(
            mimeType = "image/png",
            relativePath = Environment.DIRECTORY_PICTURES + "/Edited",
            displayName = "override-failure-${System.nanoTime()}.png",
        ) { fd ->
            writeBytes(fd, original)
        }
        assertNotNull(uri)
        val target = requireNotNull(uri)

        val stagingFile = File.createTempFile("override-test-", ".tmp", context.cacheDir)
        try {
            val success = resolver.overrideImageStreaming(target, stagingFile) { fd ->
                writeBytes(fd, byteArrayOf(1, 2, 3))
                false
            }

            assertFalse(success)
            assertArrayEquals(original, resolver.openInputStream(target)?.use { it.readBytes() })
            assertFalse(stagingFile.exists())
        } finally {
            resolver.delete(target, null, null)
            stagingFile.delete()
        }
    }

    private fun writeBytes(fd: Int, bytes: ByteArray): Boolean =
        ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.fromFd(fd)).use { output ->
            output.write(bytes)
            output.flush()
            true
        }
}
