package com.dot.gallery.metadata

import com.dot.gallery.core.metadata.LosslessMetadataRewriter
import com.dot.gallery.core.metadata.MediaContainerFormat
import com.dot.gallery.core.metadata.MediaEssenceFingerprint
import com.dot.gallery.core.metadata.MetadataRemovalMode
import com.dot.gallery.core.metadata.MetadataVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32
import kotlin.io.path.createTempDirectory

class LosslessMetadataRewriterTest {
    @Test
    fun jpegEverythingDropsMetadataSegmentsAndKeepsScanBytes() = withFiles { source, candidate ->
        val scan = byteArrayOf(1, 2, 3, 4, 0xFF.toByte(), 0xD9.toByte())
        source.writeBytes(
            byteArrayOf(0xFF.toByte(), 0xD8.toByte()) +
                segment(0xE1, "Exif\u0000\u0000private".toByteArray(Charsets.ISO_8859_1)) +
                segment(0xFE, "comment".toByteArray()) +
                segment(0xDA, byteArrayOf(0, 1)) + scan
        )
        val before = MediaEssenceFingerprint.calculate(source, MediaContainerFormat.JPEG)

        LosslessMetadataRewriter.rewrite(source, candidate, MediaContainerFormat.JPEG, MetadataRemovalMode.EVERYTHING)

        assertFalse(candidate.readBytes().toString(Charsets.ISO_8859_1).contains("Exif"))
        assertFalse(candidate.readBytes().toString(Charsets.ISO_8859_1).contains("comment"))
        assertEquals(before, MediaEssenceFingerprint.calculate(candidate, MediaContainerFormat.JPEG))
        assertTrue(MetadataVerifier.verify(candidate, MediaContainerFormat.JPEG, MetadataRemovalMode.EVERYTHING))
    }

    @Test
    fun jpegEverythingPreservesNonMetadataPhotoshopResources() = withFiles { source, candidate ->
        val photoshop = "Photoshop 3.0\u0000".toByteArray(Charsets.ISO_8859_1) +
            photoshopResource(0x0404, "iptc".toByteArray()) +
            photoshopResource(0x07D0, "path".toByteArray())
        source.writeBytes(
            byteArrayOf(0xFF.toByte(), 0xD8.toByte()) +
                segment(0xED, photoshop) +
                segment(0xDA, byteArrayOf(0, 1)) +
                byteArrayOf(1, 2, 3, 0xFF.toByte(), 0xD9.toByte())
        )

        LosslessMetadataRewriter.rewrite(source, candidate, MediaContainerFormat.JPEG, MetadataRemovalMode.EVERYTHING)

        val output = candidate.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(output.contains("iptc"))
        assertTrue(output.contains("path"))
        assertTrue(MetadataVerifier.verify(candidate, MediaContainerFormat.JPEG, MetadataRemovalMode.EVERYTHING))
    }

    @Test
    fun pngEverythingDropsTextAndKeepsImageData() = withFiles { source, candidate ->
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        writePngChunk(output, "IHDR", ByteArray(13))
        writePngChunk(output, "tEXt", "GPS=1,2".toByteArray())
        writePngChunk(output, "IDAT", byteArrayOf(9, 8, 7))
        writePngChunk(output, "IEND", ByteArray(0))
        source.writeBytes(output.toByteArray())
        val before = MediaEssenceFingerprint.calculate(source, MediaContainerFormat.PNG)

        LosslessMetadataRewriter.rewrite(source, candidate, MediaContainerFormat.PNG, MetadataRemovalMode.EVERYTHING)

        assertFalse(candidate.readBytes().toString(Charsets.ISO_8859_1).contains("GPS"))
        assertEquals(before, MediaEssenceFingerprint.calculate(candidate, MediaContainerFormat.PNG))
        assertTrue(MetadataVerifier.verify(candidate, MediaContainerFormat.PNG, MetadataRemovalMode.EVERYTHING))
    }

    @Test
    fun webpEverythingDropsExifAndKeepsEncodedChunk() = withFiles { source, candidate ->
        val body = chunk("EXIF", "private".toByteArray()) + chunk("VP8 ", byteArrayOf(4, 5, 6, 7))
        source.writeBytes("RIFF".toByteArray() + little32(body.size + 4) + "WEBP".toByteArray() + body)
        val before = MediaEssenceFingerprint.calculate(source, MediaContainerFormat.WEBP)

        LosslessMetadataRewriter.rewrite(source, candidate, MediaContainerFormat.WEBP, MetadataRemovalMode.EVERYTHING)

        assertFalse(candidate.readBytes().toString(Charsets.ISO_8859_1).contains("private"))
        assertEquals(before, MediaEssenceFingerprint.calculate(candidate, MediaContainerFormat.WEBP))
        assertTrue(MetadataVerifier.verify(candidate, MediaContainerFormat.WEBP, MetadataRemovalMode.EVERYTHING))
    }

    @Test
    fun gifPrivacyDropsCommentAndKeepsAnimationData() = withFiles { source, candidate ->
        source.writeBytes(
            "GIF89a".toByteArray() +
                byteArrayOf(1, 0, 1, 0, 0, 0, 0) +
                byteArrayOf(0x21, 0xFE.toByte(), 3) + "gps".toByteArray() + byteArrayOf(0) +
                byteArrayOf(0x3B)
        )
        val before = MediaEssenceFingerprint.calculate(source, MediaContainerFormat.GIF)

        LosslessMetadataRewriter.rewrite(source, candidate, MediaContainerFormat.GIF, MetadataRemovalMode.PRIVACY)

        assertFalse(candidate.readBytes().toString(Charsets.ISO_8859_1).contains("gps"))
        assertEquals(before, MediaEssenceFingerprint.calculate(candidate, MediaContainerFormat.GIF))
        assertTrue(MetadataVerifier.verify(candidate, MediaContainerFormat.GIF, MetadataRemovalMode.PRIVACY))
    }

    private fun segment(marker: Int, data: ByteArray): ByteArray {
        val length = data.size + 2
        return byteArrayOf(0xFF.toByte(), marker.toByte(), (length ushr 8).toByte(), length.toByte()) + data
    }

    private fun photoshopResource(id: Int, data: ByteArray): ByteArray {
        val paddedData = if (data.size and 1 == 0) data else data + 0
        return "8BIM".toByteArray() +
            byteArrayOf((id ushr 8).toByte(), id.toByte(), 0, 0) +
            big32(data.size) +
            paddedData
    }

    private fun writePngChunk(output: ByteArrayOutputStream, type: String, data: ByteArray) {
        output.write(big32(data.size))
        val typeBytes = type.toByteArray()
        output.write(typeBytes)
        output.write(data)
        val crc = CRC32().apply {
            update(typeBytes)
            update(data)
        }
        output.write(big32(crc.value.toInt()))
    }

    private fun chunk(type: String, data: ByteArray): ByteArray {
        val padded = if (data.size % 2 == 0) data else data + 0
        return type.toByteArray() + little32(data.size) + padded
    }

    private fun big32(value: Int) = byteArrayOf(
        (value ushr 24).toByte(),
        (value ushr 16).toByte(),
        (value ushr 8).toByte(),
        value.toByte()
    )

    private fun little32(value: Int) = byteArrayOf(
        value.toByte(),
        (value ushr 8).toByte(),
        (value ushr 16).toByte(),
        (value ushr 24).toByte()
    )

    private inline fun withFiles(block: (File, File) -> Unit) {
        val directory = createTempDirectory("metadata-rewriter-").toFile()
        try {
            block(File(directory, "source"), File(directory, "candidate"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
