package com.dot.gallery.metadata

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.core.metadata.AndroidMetadataSanitizer
import com.dot.gallery.core.metadata.MetadataRemovalMode
import com.dot.gallery.core.metadata.SanitizationResult
import com.dot.gallery.core.util.ext.saveImageStreaming
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

@RunWith(AndroidJUnit4::class)
class MetadataSanitizerTest {
    @Test
    fun everythingCommitsVerifiedCandidateToSameMediaStoreItem() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val original = pngWithText("private-location")
        val uri = resolver.saveImageStreaming(
            mimeType = "image/png",
            relativePath = Environment.DIRECTORY_PICTURES + "/MetadataTests",
            displayName = "sanitize-${System.nanoTime()}.png"
        ) { fd ->
            android.os.ParcelFileDescriptor.AutoCloseOutputStream(
                android.os.ParcelFileDescriptor.fromFd(fd)
            ).use { it.write(original) }
            true
        }
        val target = requireNotNull(uri)
        try {
            val media = media(target, original.size.toLong(), "image/png")
            val result = AndroidMetadataSanitizer(context).sanitize(
                media,
                MetadataRemovalMode.EVERYTHING
            )
            val committed = resolver.openInputStream(target)?.use { it.readBytes() } ?: ByteArray(0)

            assertTrue(result is SanitizationResult.Success)
            assertFalse(committed.toString(Charsets.ISO_8859_1).contains("private-location"))
            assertTrue(committed.contentEquals(pngWithoutText()))
        } finally {
            resolver.delete(target, null, null)
        }
    }

    @Test
    fun malformedCandidateLeavesOriginalUntouched() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val original = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte())
        val uri = resolver.saveImageStreaming(
            mimeType = "image/jpeg",
            relativePath = Environment.DIRECTORY_PICTURES + "/MetadataTests",
            displayName = "sanitize-malformed-${System.nanoTime()}.jpg"
        ) { fd ->
            android.os.ParcelFileDescriptor.AutoCloseOutputStream(
                android.os.ParcelFileDescriptor.fromFd(fd)
            ).use { it.write(original) }
            true
        }
        val target = requireNotNull(uri)
        try {
            val result = AndroidMetadataSanitizer(context).sanitize(
                media(target, original.size.toLong(), "image/jpeg"),
                MetadataRemovalMode.EVERYTHING
            )

            assertTrue(result is SanitizationResult.CommitFailed)
            assertArrayEquals(original, resolver.openInputStream(target)?.use { it.readBytes() })
        } finally {
            resolver.delete(target, null, null)
        }
    }

    private fun media(uri: android.net.Uri, size: Long, mime: String) = Media.UriMedia(
        id = System.nanoTime(),
        label = if (mime == "image/png") "fixture.png" else "fixture.jpg",
        uri = uri,
        path = "",
        relativePath = Environment.DIRECTORY_PICTURES + "/MetadataTests/",
        albumID = 0,
        albumLabel = "MetadataTests",
        timestamp = System.currentTimeMillis() / 1000,
        fullDate = "",
        mimeType = mime,
        favorite = 0,
        trashed = 0,
        size = size
    )

    private fun pngWithText(value: String): ByteArray = png(value)

    private fun pngWithoutText(): ByteArray = png(null)

    private fun png(text: String?): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        writeChunk(output, "IHDR", byteArrayOf(0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0))
        if (text != null) writeChunk(output, "tEXt", "Comment\u0000$text".toByteArray())
        writeChunk(output, "IDAT", byteArrayOf(0x78, 0x01, 0x01, 0x04, 0x00, 0xFB.toByte(), 0xFF.toByte(), 0, 0, 0, 0, 0, 4, 0, 1))
        writeChunk(output, "IEND", ByteArray(0))
        return output.toByteArray()
    }

    private fun writeChunk(output: ByteArrayOutputStream, type: String, data: ByteArray) {
        output.write(be32(data.size))
        val typeBytes = type.toByteArray()
        output.write(typeBytes)
        output.write(data)
        val crc = CRC32().apply {
            update(typeBytes)
            update(data)
        }
        output.write(be32(crc.value.toInt()))
    }

    private fun be32(value: Int) = byteArrayOf(
        (value ushr 24).toByte(),
        (value ushr 16).toByte(),
        (value ushr 8).toByte(),
        value.toByte()
    )
}
