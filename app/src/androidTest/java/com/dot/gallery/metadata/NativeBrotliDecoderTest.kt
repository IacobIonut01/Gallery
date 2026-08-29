package com.dot.gallery.metadata

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.core.sandbox.NativeBrotliDecoder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Base64

@RunWith(AndroidJUnit4::class)
class NativeBrotliDecoderTest {
    @Test
    fun decodesJxlExifPayloadWithinBound() {
        val compressed = Base64.getDecoder().decode(COMPRESSED_EXIF_BASE64)
        val expected = byteArrayOf(0, 0, 0, 0) + Base64.getDecoder().decode(TIFF_BASE64)

        assertArrayEquals(expected, NativeBrotliDecoder.decompress(compressed, expected.size))
        assertNull(NativeBrotliDecoder.decompress(compressed, expected.size - 1))
    }

    @Test
    fun growsOutputBufferForHighlyCompressedPayload() {
        val compressed = Base64.getDecoder().decode(COMPRESSED_REPEATED_BASE64)
        val expected = ByteArray(4096) { 'A'.code.toByte() }

        assertArrayEquals(expected, NativeBrotliDecoder.decompress(compressed, expected.size))
    }

    companion object {
        private const val TIFF_BASE64 =
            "TU0AKgAAAAgAAYglAAQAAAABAAAAGgAAAAAABAABAAIAAAACTgAAAAACAAUAAAADAAAAUAADAAIAAAACRQAAAAAEAAUAAAADAAAAaAAAAAAAAAAzAAAAAQAAAB4AAAABAAAAAAAAAAEAAAAAAAAAAQAAAAcAAAABAAAADAAAAAE="
        private const val COMPRESSED_EXIF_BASE64 =
            "H4MA+If4r92+f3dDEJKgKaqQRUaJUlQ66BQDjHDfDszy7zqCFXRRqAfKQMOrKdQF3UDDBzgRbAggACcECw=="
        private const val COMPRESSED_REPEATED_BASE64 = "H/8P+CWC4rFAIPcAAA=="
    }
}
