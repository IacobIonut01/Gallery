/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.sandbox

import android.app.Service
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.mov.media.QuickTimeVideoDirectory
import com.drew.metadata.mp4.media.Mp4VideoDirectory
import com.drew.metadata.xmp.XmpDirectory
import com.drew.metadata.xmp.XmpReader
import com.dot.gallery.data.entity.isRedactedCoordinate
import com.dot.gallery.domain.model.parseCaptureTimestamp
import com.dot.gallery.core.logging.printError
import com.dot.gallery.core.logging.printWarn
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.dot.gallery.domain.model.Album
import com.dot.gallery.core.util.and
import com.dot.gallery.core.util.like
import com.dot.gallery.core.util.or
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.cloud.core.matches
import com.dot.gallery.domain.util.isVideo

/**
 * Recovers EXIF/XMP from JPEG XL boxes that metadata-extractor can't parse directly. Both direct
 * metadata boxes and their standard Brotli-compressed `brob` representation are supported. Parsing
 * stays inside the isolated process so a malformed file can't reach the main app.
 *
 * EXIF bytes are normalized to start at the TIFF header (`II*\u0000` / `MM\u0000*`) before they are
 * handed to [ImageMetadataReader.readMetadata]. XMP bytes are merged through [XmpReader].
 */
object JxlBoxMetadataExtractor {

    private const val EXIF_BOX_TYPE = "Exif"
    private const val XMP_BOX_TYPE = "xml "
    private const val BROTLI_BOX_TYPE = "brob"
    private const val MAX_METADATA_BYTES = 8 * 1024 * 1024
    private const val MAX_METADATA_BOXES = 64
    private val metadataBoxTypes = setOf(EXIF_BOX_TYPE, XMP_BOX_TYPE)

    /** Extracts all supported embedded metadata, or null if none is present in the scanned input. */
    fun extract(
        bytes: ByteArray,
        decompressor: (ByteArray, Int) -> ByteArray? = NativeBrotliDecoder::decompress
    ): Metadata? {
        val metadata = extractMetadata(ByteArrayInputStream(bytes), decompressor)
        return mergePreamble(bytes, metadata)
    }

    fun extract(
        input: InputStream,
        decompressor: (ByteArray, Int) -> ByteArray? = NativeBrotliDecoder::decompress
    ): Metadata? = extractMetadata(input, decompressor).takeIf { it.directoryCount > 0 }

    fun mergePreamble(bytes: ByteArray, metadata: Metadata? = null): Metadata? {
        val merged = metadata ?: Metadata()
        if (!containsExif(merged)) findExifPreamble(bytes)?.let { mergeTiff(it, merged) }
        return merged.takeIf { it.directoryCount > 0 }
    }

    /** Iterates top-level JPEG XL boxes and transparently expands supported `brob` boxes. */
    private fun extractMetadata(
        input: InputStream,
        decompressor: (ByteArray, Int) -> ByteArray?
    ): Metadata {
        val metadata = Metadata()
        var metadataBudget = MAX_METADATA_BYTES
        var compressedBudget = MAX_METADATA_BYTES
        repeat(MAX_METADATA_BOXES) {
            val header = readHeader(input) ?: return metadata
            when {
                header.type in metadataBoxTypes -> {
                    val payload = readBounded(input, header.payloadSize, metadataBudget)
                    if (payload != null) {
                        metadataBudget -= payload.size
                        mergePayload(header.type, payload, metadata)
                    }
                }
                header.type == BROTLI_BOX_TYPE -> {
                    if (header.payloadSize != null && header.payloadSize < 4) {
                        input.skipExact(header.payloadSize)
                    } else {
                        val wrapped = input.readExactOrNull(4) ?: return metadata
                        val wrappedType = wrapped.toString(Charsets.ISO_8859_1)
                        val compressedSize = header.payloadSize?.minus(4)
                        val compressed = if (wrappedType in metadataBoxTypes) {
                            readBounded(input, compressedSize, compressedBudget)
                        } else {
                            if (compressedSize == null) return metadata
                            input.skipExact(compressedSize)
                            null
                        }
                        if (compressed != null) {
                            compressedBudget -= compressed.size
                            decompressor(compressed, metadataBudget)?.let { payload ->
                                if (payload.size <= metadataBudget) {
                                    metadataBudget -= payload.size
                                    mergePayload(wrappedType, payload, metadata)
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (header.payloadSize == null) return metadata
                    input.skipExact(header.payloadSize)
                }
            }
            if (header.payloadSize == null || metadataBudget == 0) return metadata
        }
        return metadata
    }

    private fun mergePayload(type: String, payload: ByteArray, metadata: Metadata) {
        when (type) {
            EXIF_BOX_TYPE -> mergeExif(payload, metadata)
            XMP_BOX_TYPE -> XmpReader().extract(payload, metadata)
        }
    }

    private fun readHeader(input: InputStream): BoxHeader? {
        val sizeBytes = input.readExactOrNull(4) ?: return null
        val type = input.readExactOrNull(4)?.toString(Charsets.ISO_8859_1) ?: return null
        val shortSize = readU32(sizeBytes, 0)
        if (shortSize == 0L) return BoxHeader(type, null)
        if (shortSize == 1L) {
            val extended = input.readExactOrNull(8) ?: return null
            val size = readU64(extended, 0)
            return if (size >= 16) BoxHeader(type, size - 16) else null
        }
        return if (shortSize >= 8) BoxHeader(type, shortSize - 8) else null
    }

    private fun readBounded(input: InputStream, size: Long?, limit: Int): ByteArray? {
        if (limit <= 0) {
            input.skipExact(size)
            return null
        }
        if (size == null) return input.readToEnd(limit)
        if (size > limit || size > Int.MAX_VALUE) {
            input.skipExact(size)
            return null
        }
        return input.readExactOrNull(size.toInt())
    }

    /**
     * JXL `Exif` box payload begins with a 4-byte big-endian offset to the TIFF header (usually 0),
     * per ISO/IEC 18181-2. Some writers instead begin directly with the TIFF header.
     */
    private fun mergeExif(payload: ByteArray, metadata: Metadata): Boolean {
        val tiff = if (payload.size >= 4) {
            val tiffStart = 4L + readU32(payload, 0)
            if (tiffStart < payload.size && isTiffHeader(payload, tiffStart.toInt())) {
                payload.copyOfRange(tiffStart.toInt(), payload.size)
            } else if (isTiffHeader(payload, 0)) {
                payload
            } else {
                null
            }
        } else {
            null
        }
        return tiff?.let { mergeTiff(it, metadata) } ?: false
    }

    private fun mergeTiff(tiff: ByteArray, metadata: Metadata): Boolean {
        val parsed = runCatching {
            ImageMetadataReader.readMetadata(ByteArrayInputStream(tiff))
        }.getOrNull() ?: return false
        parsed.directories.forEach(metadata::addDirectory)
        return parsed.directoryCount > 0
    }

    /** Scans for a JPEG-style "Exif\u0000\u0000" preamble immediately followed by a TIFF header. */
    private fun findExifPreamble(bytes: ByteArray): ByteArray? {
        val n = bytes.size
        var i = 0
        while (i + 6 < n) {
            if (bytes[i] == 'E'.code.toByte() && bytes[i + 1] == 'x'.code.toByte() &&
                bytes[i + 2] == 'i'.code.toByte() && bytes[i + 3] == 'f'.code.toByte() &&
                bytes[i + 4] == 0x00.toByte() && bytes[i + 5] == 0x00.toByte()
            ) {
                val tiffStart = i + 6
                if (isTiffHeader(bytes, tiffStart)) return bytes.copyOfRange(tiffStart, n)
            }
            i++
        }
        return null
    }

    private fun containsExif(metadata: Metadata): Boolean =
        metadata.containsDirectoryOfType(ExifIFD0Directory::class.java) ||
                metadata.containsDirectoryOfType(ExifSubIFDDirectory::class.java) ||
                metadata.containsDirectoryOfType(GpsDirectory::class.java)

    private fun InputStream.readExactOrNull(size: Int): ByteArray? {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val count = read(bytes, offset, size - offset)
            if (count <= 0) return null
            offset += count
        }
        return bytes
    }

    private fun InputStream.readToEnd(limit: Int): ByteArray? {
        val output = ByteArrayOutputStream(minOf(limit, DEFAULT_BUFFER_SIZE))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = read(buffer)
            if (count < 0) return output.toByteArray()
            if (output.size() + count > limit) return null
            output.write(buffer, 0, count)
        }
    }

    private fun InputStream.skipExact(size: Long?): Boolean {
        if (size == null) return true
        var remaining = size
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else if (read() >= 0) {
                remaining--
            } else {
                return false
            }
        }
        return true
    }

    /** True if [off] points at a TIFF header: little-endian (II 2A 00) or big-endian (MM 00 2A). */
    private fun isTiffHeader(bytes: ByteArray, off: Int): Boolean {
        if (off < 0 || off + 4 > bytes.size) return false
        val le = bytes[off] == 0x49.toByte() && bytes[off + 1] == 0x49.toByte() &&
                bytes[off + 2] == 0x2A.toByte() && bytes[off + 3] == 0x00.toByte()
        val be = bytes[off] == 0x4D.toByte() && bytes[off + 1] == 0x4D.toByte() &&
                bytes[off + 2] == 0x00.toByte() && bytes[off + 3] == 0x2A.toByte()
        return le || be
    }

    private fun readU32(bytes: ByteArray, off: Int): Long =
        ((bytes[off].toLong() and 0xFF) shl 24) or
                ((bytes[off + 1].toLong() and 0xFF) shl 16) or
                ((bytes[off + 2].toLong() and 0xFF) shl 8) or
                (bytes[off + 3].toLong() and 0xFF)

    private fun readU64(bytes: ByteArray, off: Int): Long {
        if (bytes[off].toInt() and 0x80 != 0) return -1
        var value = 0L
        for (k in 0 until 8) {
            value = (value shl 8) or (bytes[off + k].toLong() and 0xFF)
        }
        return value
    }

    private data class BoxHeader(val type: String, val payloadSize: Long?)
}
