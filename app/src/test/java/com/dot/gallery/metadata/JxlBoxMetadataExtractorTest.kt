package com.dot.gallery.metadata

import com.dot.gallery.core.sandbox.JxlBoxMetadataExtractor
import com.drew.metadata.Metadata
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.xmp.XmpDirectory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.Base64
import java.util.Collections

class JxlBoxMetadataExtractorTest {
    @Test
    fun extractsGpsFromDirectAndCompressedExifBoxes() {
        val payload = byteArrayOf(0, 0, 0, 0) + decode(TIFF_BASE64)
        assertGps(JxlBoxMetadataExtractor.extract(box("Exif", payload)))

        val compressedPayload = decode(COMPRESSED_EXIF_BASE64)
        val compressed = box("brob", "Exif".toByteArray() + compressedPayload)
        val metadata = JxlBoxMetadataExtractor.extract(compressed) { bytes, limit ->
            assertArrayEquals(compressedPayload, bytes)
            assertEquals(8 * 1024 * 1024, limit)
            payload
        }
        assertGps(metadata)
    }

    @Test
    fun extractsXmpFromDirectAndCompressedXmlBoxes() {
        val xmp = XMP.toByteArray()
        assertXmp(JxlBoxMetadataExtractor.extract(box("xml ", xmp)))

        val compressedPayload = decode(COMPRESSED_XMP_BASE64)
        val compressed = box("brob", "xml ".toByteArray() + compressedPayload)
        val metadata = JxlBoxMetadataExtractor.extract(compressed) { bytes, _ ->
            assertArrayEquals(compressedPayload, bytes)
            xmp
        }
        assertXmp(metadata)
    }

    @Test
    fun findsExifAfterLargeCodestreamWithoutBufferingIt() {
        val payload = byteArrayOf(0, 0, 0, 0) + decode(TIFF_BASE64)
        val codestreamSize = 9L * 1024 * 1024
        val input = SequenceInputStream(Collections.enumeration(listOf(
            ByteArrayInputStream(boxHeader("jxlc", codestreamSize)),
            ZeroInputStream(codestreamSize),
            ByteArrayInputStream(box("Exif", payload))
        )))

        assertGps(JxlBoxMetadataExtractor.extract(input))
    }

    @Test
    fun acceptsExtendedAndUnboundedExifBoxes() {
        val payload = byteArrayOf(0, 0, 0, 0) + decode(TIFF_BASE64)

        assertGps(JxlBoxMetadataExtractor.extract(extendedBox("Exif", payload)))
        assertGps(JxlBoxMetadataExtractor.extract(unboundedBox("Exif", payload)))
    }

    @Test
    fun doesNotDrainUnboundedFinalCodestream() {
        val input = SequenceInputStream(Collections.enumeration(listOf(
            ByteArrayInputStream(unboundedBox("jxlc", byteArrayOf())),
            object : InputStream() {
                override fun read(): Int = error("unbounded codestream payload must not be read")
            }
        )))

        assertNull(JxlBoxMetadataExtractor.extract(input))
    }

    @Test
    fun sharesOneDecompressedMetadataBudgetAcrossBoxes() {
        val compressed = box("brob", "Exif".toByteArray() + byteArrayOf(1))
        val limits = mutableListOf<Int>()

        JxlBoxMetadataExtractor.extract(compressed + compressed) { _, limit ->
            limits += limit
            ByteArray(minOf(limit, 5 * 1024 * 1024))
        }

        assertEquals(listOf(8 * 1024 * 1024, 3 * 1024 * 1024), limits)
    }

    private fun assertGps(metadata: Metadata?) {
        val gps = metadata?.getFirstDirectoryOfType(GpsDirectory::class.java)?.geoLocation
        assertNotNull(gps)
        assertEquals(51.5, gps!!.latitude, 0.0001)
        assertEquals(0.12, gps.longitude, 0.0001)
    }

    private fun assertXmp(metadata: Metadata?) {
        val directory = metadata?.getFirstDirectoryOfType(XmpDirectory::class.java)
        assertNotNull(directory)
        assertTrue(directory!!.xmpProperties.any { it.value == "image/jxl" })
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

    private fun boxHeader(type: String, payloadSize: Long): ByteArray = ByteArrayOutputStream().use { output ->
        val size = payloadSize + 8
        output.write(byteArrayOf(
            (size ushr 24).toByte(),
            (size ushr 16).toByte(),
            (size ushr 8).toByte(),
            size.toByte()
        ))
        output.write(type.toByteArray(Charsets.ISO_8859_1))
        output.toByteArray()
    }

    private fun extendedBox(type: String, data: ByteArray): ByteArray = ByteArrayOutputStream().use { output ->
        output.write(byteArrayOf(0, 0, 0, 1))
        output.write(type.toByteArray(Charsets.ISO_8859_1))
        val size = data.size.toLong() + 16
        for (shift in 56 downTo 0 step 8) output.write((size ushr shift).toInt())
        output.write(data)
        output.toByteArray()
    }

    private fun unboundedBox(type: String, data: ByteArray): ByteArray =
        byteArrayOf(0, 0, 0, 0) + type.toByteArray(Charsets.ISO_8859_1) + data

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    private class ZeroInputStream(private var remaining: Long) : InputStream() {
        override fun read(): Int {
            if (remaining == 0L) return -1
            remaining--
            return 0
        }

        override fun skip(count: Long): Long {
            val skipped = minOf(count, remaining)
            remaining -= skipped
            return skipped
        }
    }

    companion object {
        private const val TIFF_BASE64 =
            "TU0AKgAAAAgAAYglAAQAAAABAAAAGgAAAAAABAABAAIAAAACTgAAAAACAAUAAAADAAAAUAADAAIAAAACRQAAAAAEAAUAAAADAAAAaAAAAAAAAAAzAAAAAQAAAB4AAAABAAAAAAAAAAEAAAAAAAAAAQAAAAcAAAABAAAADAAAAAE="
        private const val COMPRESSED_EXIF_BASE64 =
            "H4MA+If4r92+f3dDEJKgKaqQRUaJUlQ66BQDjHDfDszy7zqCFXRRqAfKQMOrKdQF3UDDBzgRbAggACcECw=="
        private const val XMP =
            "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:Description xmlns:dc=\"http://purl.org/dc/elements/1.1/\" dc:format=\"image/jxl\"/></rdf:RDF></x:xmpmeta>"
        private const val COMPRESSED_XMP_BASE64 =
            "H84AIGR1TtmBbrtNonoef+EA8OkhB4DVWiuQoC2JD/jt21mf+vWGjzxekSzyeBw+/We/eZvrKz8VNb8g2tWskGUiEf1RNTZLUqd1VE0QQ5xqdtsjAGXIGJjg/Jcr/2A5ZYwTn8SKqqTe7mPiqkqC8CCNmgSrSvveMIx0p+GbBEVPiaGiMDwC"
    }
}
