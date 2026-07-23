package com.dot.gallery.metadata

import com.dot.gallery.core.metadata.BoxMetadataRewriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.path.createTempDirectory

class BoxMetadataRewriterTest {
    @Test
    fun removesMetadataBoxesAndPreservesCodestream() = withFiles { source, candidate ->
        val codestream = ByteArray(DEFAULT_BUFFER_SIZE * 3) { (it % 251).toByte() }
        source.writeBytes(
            box("jP  ", byteArrayOf(0x0D, 0x0A, 0x87.toByte(), 0x0A)) +
                box("xml ", "<metadata>private</metadata>".toByteArray()) +
                box("jp2c", codestream)
        )
        val before = BoxMetadataRewriter.essenceFingerprint(source)

        BoxMetadataRewriter.rewriteEverything(source, candidate)

        assertTrue(BoxMetadataRewriter.containsMetadata(source))
        assertTrue(BoxMetadataRewriter.hasEssence(candidate))
        assertFalse(BoxMetadataRewriter.containsMetadata(candidate))
        assertEquals(before, BoxMetadataRewriter.essenceFingerprint(candidate))
        assertFalse(candidate.readBytes().toString(Charsets.ISO_8859_1).contains("private"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnboundedBoxes() = withFiles { source, candidate ->
        source.writeBytes(byteArrayOf(0, 0, 0, 0) + "jp2c".toByteArray() + byteArrayOf(1, 2, 3))
        BoxMetadataRewriter.rewriteEverything(source, candidate)
    }

    private fun box(type: String, data: ByteArray): ByteArray = ByteArrayOutputStream().use { output ->
        val size = data.size + 8
        output.write(byteArrayOf(
            (size ushr 24).toByte(),
            (size ushr 16).toByte(),
            (size ushr 8).toByte(),
            size.toByte()
        ))
        output.write(type.toByteArray(Charsets.ISO_8859_1))
        output.write(data)
        output.toByteArray()
    }

    private inline fun withFiles(block: (File, File) -> Unit) {
        val directory = createTempDirectory("box-metadata-").toFile()
        try {
            block(File(directory, "source"), File(directory, "candidate"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
