package com.dot.gallery.metadata

import com.dot.gallery.core.metadata.MediaContainerFormat
import com.dot.gallery.core.metadata.MediaFormatDetector
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFormatDetectorTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun detectsCommonRasterFormatsByMagic() {
        assertEquals(MediaContainerFormat.JPEG, MediaFormatDetector.detect(bytes(0xFF, 0xD8, 0xFF)))
        assertEquals(MediaContainerFormat.PNG, MediaFormatDetector.detect(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
        assertEquals(MediaContainerFormat.GIF, MediaFormatDetector.detect("GIF89a".toByteArray()))
        assertEquals(MediaContainerFormat.BMP, MediaFormatDetector.detect("BM".toByteArray()))
    }

    @Test
    fun detectsWebpAndAviFromRiffFormType() {
        val webp = "RIFF0000WEBP".toByteArray()
        val avi = "RIFF0000AVI ".toByteArray()

        assertEquals(MediaContainerFormat.WEBP, MediaFormatDetector.detect(webp))
        assertEquals(MediaContainerFormat.AVI, MediaFormatDetector.detect(avi))
    }

    @Test
    fun detectsBmffBrands() {
        assertEquals(MediaContainerFormat.AVIF, MediaFormatDetector.detect(ftyp("avif")))
        assertEquals(MediaContainerFormat.HEIF, MediaFormatDetector.detect(ftyp("heic")))
        assertEquals(MediaContainerFormat.QUICKTIME, MediaFormatDetector.detect(ftyp("qt  ")))
        assertEquals(MediaContainerFormat.MP4, MediaFormatDetector.detect(ftyp("isom")))
    }

    @Test
    fun detectsSvgWithXmlPrefix() {
        val svg = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"/>".toByteArray()
        assertEquals(MediaContainerFormat.SVG, MediaFormatDetector.detect(svg))
    }

    @Test
    fun fallsBackToVideoHintsOnlyAfterMagicFails() {
        assertEquals(
            MediaContainerFormat.MATROSKA,
            MediaFormatDetector.detect(ByteArray(0), mimeType = "video/x-matroska", fileName = "clip.mkv")
        )
        assertEquals(MediaContainerFormat.UNKNOWN, MediaFormatDetector.detect(ByteArray(0)))
    }

    private fun ftyp(brand: String): ByteArray = byteArrayOf(0, 0, 0, 24) + "ftyp".toByteArray() + brand.toByteArray() + ByteArray(12)
}
