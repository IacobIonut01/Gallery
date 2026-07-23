package com.dot.gallery.core.metadata

import androidx.exifinterface.media.ExifInterface
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

internal object MetadataVerifier {
    fun verify(file: File, format: MediaContainerFormat, mode: MetadataRemovalMode): Boolean {
        if (file.length() <= 0L) return false
        return when (format) {
            MediaContainerFormat.JPEG -> verifyJpeg(file, mode) && verifyExif(file, mode)
            MediaContainerFormat.PNG -> verifyPng(file, mode) && verifyExif(file, mode)
            MediaContainerFormat.WEBP -> verifyWebp(file, mode) && verifyExif(file, mode)
            MediaContainerFormat.GIF -> mode == MetadataRemovalMode.LOCATION || !hasGifComment(file)
            MediaContainerFormat.BMP -> true
            MediaContainerFormat.JP2,
            MediaContainerFormat.JXL -> mode == MetadataRemovalMode.EVERYTHING &&
                BoxMetadataRewriter.hasEssence(file) &&
                !BoxMetadataRewriter.containsMetadata(file)
            else -> false
        }
    }

    private fun verifyExif(file: File, mode: MetadataRemovalMode): Boolean {
        if (mode == MetadataRemovalMode.EVERYTHING) return true
        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull() ?: return true
        if (exif.latLong != null) return false
        if (mode == MetadataRemovalMode.PRIVACY) {
            val privacyTags = arrayOf(
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_BODY_SERIAL_NUMBER,
                ExifInterface.TAG_CAMERA_OWNER_NAME,
                ExifInterface.TAG_COPYRIGHT,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_IMAGE_DESCRIPTION,
                ExifInterface.TAG_IMAGE_UNIQUE_ID,
                ExifInterface.TAG_LENS_SERIAL_NUMBER,
                ExifInterface.TAG_MAKER_NOTE,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_USER_COMMENT
            )
            if (privacyTags.any { exif.getAttribute(it) != null }) return false
        }
        return true
    }

    private fun verifyJpeg(file: File, mode: MetadataRemovalMode): Boolean {
        BufferedInputStream(FileInputStream(file)).use { input ->
            if (input.read() != 0xFF || input.read() != 0xD8) return false
            while (true) {
                if (input.read() != 0xFF) return false
                var marker = input.read()
                while (marker == 0xFF) marker = input.read()
                if (marker < 0 || marker == 0xD9) return marker == 0xD9
                if (marker == 0xDA) return true
                if (marker in 0xD0..0xD7 || marker == 0x01) continue
                val length = input.readUnsignedShort()
                if (length < 2) return false
                val data = input.readExact(length - 2)
                val exif = marker == 0xE1 && data.startsWithAscii("Exif\u0000\u0000")
                val xmp = marker == 0xE1 && data.startsWithAscii("http://ns.adobe.com/")
                val app13 = marker == 0xED
                val photoshop = app13 && data.startsWithAscii("Photoshop 3.0\u0000")
                val iptc = app13 && (!photoshop || LosslessMetadataRewriter.hasPhotoshopMetadata(data))
                val commentOrProvenance = marker == 0xFE || marker == 0xEB
                when (mode) {
                    MetadataRemovalMode.LOCATION -> if (xmp || iptc) return false
                    MetadataRemovalMode.PRIVACY -> if (xmp || iptc || commentOrProvenance) return false
                    MetadataRemovalMode.EVERYTHING -> if (exif || xmp || iptc || commentOrProvenance) {
                        return false
                    }
                }
            }
        }
    }

    private fun verifyPng(file: File, mode: MetadataRemovalMode): Boolean {
        BufferedInputStream(FileInputStream(file)).use { input ->
            input.readExact(8)
            while (true) {
                val lengthBytes = input.readExactOrNull(4) ?: return false
                val length = lengthBytes.toUInt32()
                if (length > Int.MAX_VALUE) return false
                val name = input.readExact(4).toString(Charsets.ISO_8859_1)
                input.readExact(length.toInt())
                input.readExact(4)
                val text = name in setOf("iTXt", "tEXt", "zTXt")
                when (mode) {
                    MetadataRemovalMode.LOCATION -> if (text) return false
                    MetadataRemovalMode.PRIVACY -> if (text || name in setOf("caBX", "tIME")) return false
                    MetadataRemovalMode.EVERYTHING -> if (
                        text || name in setOf("caBX", "tIME", "eXIf")
                    ) return false
                }
                if (name == "IEND") return true
            }
        }
    }

    private fun verifyWebp(file: File, mode: MetadataRemovalMode): Boolean {
        BufferedInputStream(FileInputStream(file)).use { input ->
            if (input.readExact(4).toString(Charsets.US_ASCII) != "RIFF") return false
            input.readExact(4)
            if (input.readExact(4).toString(Charsets.US_ASCII) != "WEBP") return false
            while (true) {
                val name = input.readExactOrNull(4)?.toString(Charsets.US_ASCII) ?: return true
                val size = input.readExact(4).toLittleUInt32()
                if (size > Int.MAX_VALUE) return false
                input.readExact(size.toInt())
                if (size and 1L == 1L) input.read()
                if (name == "XMP " || (mode == MetadataRemovalMode.EVERYTHING && name == "EXIF")) {
                    return false
                }
            }
        }
    }

    private fun hasGifComment(file: File): Boolean = BufferedInputStream(FileInputStream(file)).use { input ->
        var previous = -1
        while (true) {
            val current = input.read()
            if (current < 0) return@use false
            if (previous == 0x21 && current == 0xFE) return@use true
            previous = current
        }
        @Suppress("UNREACHABLE_CODE")
        false
    }

    private fun InputStream.readUnsignedShort(): Int {
        val high = read()
        val low = read()
        if (high < 0 || low < 0) throw EOFException()
        return high shl 8 or low
    }

    private fun InputStream.readExact(size: Int): ByteArray = readExactOrNull(size) ?: throw EOFException()

    private fun InputStream.readExactOrNull(size: Int): ByteArray? {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val count = read(bytes, offset, size - offset)
            if (count < 0) return if (offset == 0) null else throw EOFException()
            offset += count
        }
        return bytes
    }

    private fun ByteArray.startsWithAscii(value: String): Boolean {
        val prefix = value.toByteArray(Charsets.ISO_8859_1)
        return size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
    }

    private fun ByteArray.toUInt32(): Long =
        ((this[0].toLong() and 0xFF) shl 24) or
            ((this[1].toLong() and 0xFF) shl 16) or
            ((this[2].toLong() and 0xFF) shl 8) or
            (this[3].toLong() and 0xFF)

    private fun ByteArray.toLittleUInt32(): Long =
        (this[0].toLong() and 0xFF) or
            ((this[1].toLong() and 0xFF) shl 8) or
            ((this[2].toLong() and 0xFF) shl 16) or
            ((this[3].toLong() and 0xFF) shl 24)
}
