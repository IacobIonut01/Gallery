package com.dot.gallery.core.metadata

import java.io.InputStream

object MediaFormatDetector {
    private const val SNIFF_BYTES = 512

    fun detect(input: InputStream, mimeType: String? = null, fileName: String? = null): MediaContainerFormat {
        val header = ByteArray(SNIFF_BYTES)
        val length = input.read(header).coerceAtLeast(0)
        return detect(header, length, mimeType, fileName)
    }

    fun detect(
        header: ByteArray,
        length: Int = header.size,
        mimeType: String? = null,
        fileName: String? = null
    ): MediaContainerFormat {
        val safeLength = length.coerceIn(0, header.size)
        if (matches(header, safeLength, 0, 0xFF, 0xD8, 0xFF)) return MediaContainerFormat.JPEG
        if (matches(header, safeLength, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return MediaContainerFormat.PNG
        if (ascii(header, safeLength, 0, "RIFF") && ascii(header, safeLength, 8, "WEBP")) return MediaContainerFormat.WEBP
        if (ascii(header, safeLength, 0, "GIF87a") || ascii(header, safeLength, 0, "GIF89a")) return MediaContainerFormat.GIF
        if (ascii(header, safeLength, 0, "BM")) return MediaContainerFormat.BMP
        if (isTiff(header, safeLength)) return MediaContainerFormat.TIFF
        if (ascii(header, safeLength, 0, "8BPS")) return MediaContainerFormat.PSD
        if (matches(header, safeLength, 0, 0x00, 0x00, 0x00, 0x0C, 0x6A, 0x50, 0x20, 0x20, 0x0D, 0x0A, 0x87, 0x0A)) return MediaContainerFormat.JP2
        if (matches(header, safeLength, 0, 0xFF, 0x4F, 0xFF, 0x51)) return MediaContainerFormat.J2K
        if (matches(header, safeLength, 0, 0xFF, 0x0A) || ascii(header, safeLength, 4, "JXL ")) return MediaContainerFormat.JXL
        if (ascii(header, safeLength, 0, "OggS")) return MediaContainerFormat.OGG
        if (ascii(header, safeLength, 0, "\u001aE\u00df\u00a3")) {
            val text = header.copyOf(safeLength).toString(Charsets.ISO_8859_1).lowercase()
            return if ("webm" in text) MediaContainerFormat.WEBM else MediaContainerFormat.MATROSKA
        }
        if (ascii(header, safeLength, 0, "RIFF") && ascii(header, safeLength, 8, "AVI ")) return MediaContainerFormat.AVI
        if (safeLength >= 188 && header[0] == 0x47.toByte() && (safeLength < 376 || header[188] == 0x47.toByte())) return MediaContainerFormat.MPEG_TS
        detectBmff(header, safeLength)?.let { return it }
        if (looksLikeSvg(header, safeLength)) return MediaContainerFormat.SVG
        return detectFromHints(mimeType, fileName)
    }

    private fun detectBmff(header: ByteArray, length: Int): MediaContainerFormat? {
        if (!ascii(header, length, 4, "ftyp")) return null
        val brands = header.copyOfRange(8, length.coerceAtMost(64)).toString(Charsets.ISO_8859_1).lowercase()
        return when {
            listOf("avif", "avis").any { it in brands } -> MediaContainerFormat.AVIF
            listOf("heic", "heix", "hevc", "hevx", "heim", "heis", "mif1", "msf1").any { it in brands } -> MediaContainerFormat.HEIF
            listOf("3gp", "3g2").any { it in brands } -> MediaContainerFormat.THREE_GPP
            "qt  " in brands -> MediaContainerFormat.QUICKTIME
            else -> MediaContainerFormat.MP4
        }
    }

    private fun detectFromHints(mimeType: String?, fileName: String?): MediaContainerFormat {
        val mime = mimeType.orEmpty().lowercase()
        val extension = fileName.orEmpty().substringAfterLast('.', "").lowercase()
        return when {
            mime == "image/svg+xml" || extension == "svg" -> MediaContainerFormat.SVG
            mime.startsWith("video/") && extension in setOf("mkv") -> MediaContainerFormat.MATROSKA
            mime.startsWith("video/") && extension in setOf("webm") -> MediaContainerFormat.WEBM
            mime.startsWith("video/") && extension in setOf("avi") -> MediaContainerFormat.AVI
            mime.startsWith("video/") && extension in setOf("ts", "m2ts", "mts") -> MediaContainerFormat.MPEG_TS
            mime.startsWith("video/") && extension in setOf("ogv", "ogg") -> MediaContainerFormat.OGG
            mime.startsWith("video/") -> MediaContainerFormat.MP4
            else -> MediaContainerFormat.UNKNOWN
        }
    }

    private fun isTiff(header: ByteArray, length: Int): Boolean =
        matches(header, length, 0, 0x49, 0x49, 0x2A, 0x00) ||
            matches(header, length, 0, 0x4D, 0x4D, 0x00, 0x2A) ||
            matches(header, length, 0, 0x49, 0x49, 0x2B, 0x00) ||
            matches(header, length, 0, 0x4D, 0x4D, 0x00, 0x2B)

    private fun looksLikeSvg(header: ByteArray, length: Int): Boolean {
        if (length == 0) return false
        val text = header.copyOf(length).toString(Charsets.UTF_8).trimStart('\uFEFF', ' ', '\t', '\r', '\n').lowercase()
        return text.startsWith("<svg") || ((text.startsWith("<?xml") || text.startsWith("<!--")) && "<svg" in text)
    }

    private fun ascii(header: ByteArray, length: Int, offset: Int, value: String): Boolean {
        if (offset < 0 || offset + value.length > length) return false
        return value.indices.all { header[offset + it] == value[it].code.toByte() }
    }

    private fun matches(header: ByteArray, length: Int, offset: Int, vararg values: Int): Boolean {
        if (offset < 0 || offset + values.size > length) return false
        return values.indices.all { header[offset + it] == values[it].toByte() }
    }
}
