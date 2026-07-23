package com.dot.gallery.core.metadata

import androidx.exifinterface.media.ExifInterface
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32

internal object LosslessMetadataRewriter {
    fun rewrite(source: File, candidate: File, format: MediaContainerFormat, mode: MetadataRemovalMode) {
        when (format) {
            MediaContainerFormat.JPEG -> rewriteJpeg(source, candidate, mode)
            MediaContainerFormat.PNG -> rewritePng(source, candidate, mode)
            MediaContainerFormat.WEBP -> rewriteWebp(source, candidate, mode)
            MediaContainerFormat.GIF -> rewriteGif(source, candidate, mode)
            MediaContainerFormat.BMP -> source.copyTo(candidate, overwrite = true)
            MediaContainerFormat.JP2,
            MediaContainerFormat.JXL -> {
                require(mode == MetadataRemovalMode.EVERYTHING)
                BoxMetadataRewriter.rewriteEverything(source, candidate)
            }
            else -> error("No lossless writer for $format")
        }
        if (mode != MetadataRemovalMode.EVERYTHING && format in setOf(
                MediaContainerFormat.JPEG,
                MediaContainerFormat.PNG,
                MediaContainerFormat.WEBP
            )
        ) {
            clearExifAttributes(candidate, mode)
        }
    }

    private fun rewriteJpeg(source: File, candidate: File, mode: MetadataRemovalMode) {
        BufferedInputStream(FileInputStream(source)).use { input ->
            BufferedOutputStream(FileOutputStream(candidate)).use { output ->
                require(input.read() == 0xFF && input.read() == 0xD8) { "Invalid JPEG" }
                output.write(0xFF)
                output.write(0xD8)
                while (true) {
                    val prefix = input.read()
                    if (prefix == -1) break
                    require(prefix == 0xFF) { "Invalid JPEG marker prefix" }
                    var marker = input.read()
                    while (marker == 0xFF) marker = input.read()
                    if (marker == -1) throw EOFException("Truncated JPEG marker")
                    if (marker == 0xD9) {
                        output.write(0xFF)
                        output.write(marker)
                        input.copyTo(output)
                        break
                    }
                    if (marker == 0xDA) {
                        val length = input.readUnsignedShort()
                        val data = input.readExact(length - 2)
                        output.write(0xFF)
                        output.write(marker)
                        output.writeUnsignedShort(length)
                        output.write(data)
                        input.copyTo(output)
                        break
                    }
                    if (marker in 0xD0..0xD7 || marker == 0x01) {
                        output.write(0xFF)
                        output.write(marker)
                        continue
                    }
                    val length = input.readUnsignedShort()
                    require(length >= 2) { "Invalid JPEG segment length" }
                    val data = input.readExact(length - 2)
                    val rewritten = rewriteJpegSegment(marker, data, mode)
                    if (rewritten != null) {
                        output.write(0xFF)
                        output.write(marker)
                        output.writeUnsignedShort(rewritten.size + 2)
                        output.write(rewritten)
                    }
                }
            }
        }
    }

    private fun rewriteJpegSegment(
        marker: Int,
        data: ByteArray,
        mode: MetadataRemovalMode
    ): ByteArray? {
        val exif = marker == 0xE1 && data.startsWithAscii("Exif\u0000\u0000")
        val xmp = marker == 0xE1 && (
            data.startsWithAscii("http://ns.adobe.com/xap/1.0/\u0000") ||
                data.startsWithAscii("http://ns.adobe.com/xmp/extension/\u0000")
            )
        val app13 = marker == 0xED
        val photoshop = app13 && data.startsWithAscii("Photoshop 3.0\u0000")
        val comment = marker == 0xFE
        val jumbf = marker == 0xEB
        return when (mode) {
            MetadataRemovalMode.LOCATION -> if (xmp || photoshop) null else data
            MetadataRemovalMode.PRIVACY -> if (xmp || photoshop || comment || jumbf) null else data
            MetadataRemovalMode.EVERYTHING -> when {
                exif || xmp || comment || jumbf -> null
                photoshop -> filterPhotoshopResources(data)
                app13 -> null
                else -> data
            }
        }
    }

    internal fun hasPhotoshopMetadata(data: ByteArray): Boolean {
        if (!data.startsWithAscii("Photoshop 3.0\u0000")) return false
        val prefixSize = "Photoshop 3.0\u0000".toByteArray(Charsets.ISO_8859_1).size
        var offset = prefixSize
        while (offset < data.size) {
            require(offset + 7 <= data.size) { "Truncated Photoshop resource" }
            offset += 4
            val id = (data[offset].toInt() and 0xFF) shl 8 or (data[offset + 1].toInt() and 0xFF)
            offset += 2
            val nameLength = data[offset].toInt() and 0xFF
            offset += 1 + nameLength
            if ((1 + nameLength) and 1 == 1) offset++
            require(offset + 4 <= data.size) { "Truncated Photoshop resource size" }
            val resourceSize = data.readUInt32(offset)
            offset += 4
            require(resourceSize <= Int.MAX_VALUE && offset + resourceSize <= data.size) {
                "Truncated Photoshop resource data"
            }
            offset += resourceSize.toInt()
            if (resourceSize and 1L == 1L) offset++
            if (id in photoshopMetadataResourceIds) return true
        }
        return false
    }

    private fun filterPhotoshopResources(data: ByteArray): ByteArray? {
        val prefix = "Photoshop 3.0\u0000".toByteArray(Charsets.ISO_8859_1)
        val output = java.io.ByteArrayOutputStream(data.size)
        output.write(prefix)
        var offset = prefix.size
        while (offset < data.size) {
            val start = offset
            require(offset + 7 <= data.size) { "Truncated Photoshop resource" }
            val signature = data.copyOfRange(offset, offset + 4).toString(Charsets.ISO_8859_1)
            require(signature == "8BIM" || signature == "8B64") { "Invalid Photoshop resource" }
            offset += 4
            val id = (data[offset].toInt() and 0xFF) shl 8 or (data[offset + 1].toInt() and 0xFF)
            offset += 2
            val nameLength = data[offset].toInt() and 0xFF
            offset += 1 + nameLength
            if ((1 + nameLength) and 1 == 1) offset++
            require(offset + 4 <= data.size) { "Truncated Photoshop resource size" }
            val resourceSize = data.readUInt32(offset)
            require(resourceSize <= Int.MAX_VALUE.toLong()) { "Photoshop resource too large" }
            offset += 4
            require(offset + resourceSize <= data.size) { "Truncated Photoshop resource data" }
            offset += resourceSize.toInt()
            if (resourceSize and 1L == 1L) offset++
            require(offset <= data.size) { "Invalid Photoshop resource padding" }
            if (id !in photoshopMetadataResourceIds) output.write(data, start, offset - start)
        }
        return output.toByteArray().takeIf { it.size > prefix.size }
    }

    private val photoshopMetadataResourceIds = setOf(
        0x0404,
        0x040C,
        0x040D,
        0x0410,
        0x041A,
        0x041B,
        0x041C,
        0x0421,
        0x0422,
        0x0423,
        0x0424,
        0x0425,
        0x042D,
        0x0435
    )

    private fun rewritePng(source: File, candidate: File, mode: MetadataRemovalMode) {
        BufferedInputStream(FileInputStream(source)).use { input ->
            BufferedOutputStream(FileOutputStream(candidate)).use { output ->
                val signature = input.readExact(8)
                require(signature.contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))) { "Invalid PNG" }
                output.write(signature)
                while (true) {
                    val lengthBytes = input.readExactOrNull(4) ?: break
                    val length = lengthBytes.toUInt32()
                    require(length <= Int.MAX_VALUE.toLong()) { "PNG chunk too large" }
                    val type = input.readExact(4)
                    val data = input.readExact(length.toInt())
                    input.readExact(4)
                    val name = type.toString(Charsets.ISO_8859_1)
                    if (keepPngChunk(name, mode)) writePngChunk(output, type, data)
                    if (name == "IEND") break
                }
            }
        }
    }

    private fun keepPngChunk(name: String, mode: MetadataRemovalMode): Boolean = when (mode) {
        MetadataRemovalMode.LOCATION -> name !in setOf("iTXt", "tEXt", "zTXt")
        MetadataRemovalMode.PRIVACY -> name !in setOf("iTXt", "tEXt", "zTXt", "tIME", "caBX")
        MetadataRemovalMode.EVERYTHING -> name !in setOf("eXIf", "iTXt", "tEXt", "zTXt", "tIME", "caBX")
    }

    private fun writePngChunk(output: OutputStream, type: ByteArray, data: ByteArray) {
        output.writeUInt32(data.size.toLong())
        output.write(type)
        output.write(data)
        val crc = CRC32()
        crc.update(type)
        crc.update(data)
        output.writeUInt32(crc.value)
    }

    private fun rewriteWebp(source: File, candidate: File, mode: MetadataRemovalMode) {
        BufferedInputStream(FileInputStream(source)).use { input ->
            require(input.readExact(4).toString(Charsets.US_ASCII) == "RIFF") { "Invalid WebP RIFF" }
            input.readExact(4)
            require(input.readExact(4).toString(Charsets.US_ASCII) == "WEBP") { "Invalid WebP form" }
            val body = File.createTempFile("metadata-webp-body", ".tmp", candidate.parentFile)
            try {
                BufferedOutputStream(FileOutputStream(body)).use { output ->
                    while (true) {
                        val type = input.readExactOrNull(4) ?: break
                        val sizeBytes = input.readExact(4)
                        val size = sizeBytes.toLittleUInt32()
                        require(size <= Int.MAX_VALUE.toLong()) { "WebP chunk too large" }
                        val data = input.readExact(size.toInt())
                        val pad = if (size and 1L == 1L) input.read() else -1
                        val name = type.toString(Charsets.US_ASCII)
                        if (name == "VP8X" && data.isNotEmpty()) {
                            val metadataMask = if (mode == MetadataRemovalMode.EVERYTHING) 0xF3 else 0xFB
                            data[0] = (data[0].toInt() and metadataMask).toByte()
                        }
                        if (keepWebpChunk(name, mode)) {
                            output.write(type)
                            output.writeLittleUInt32(size)
                            output.write(data)
                            if (pad >= 0) output.write(pad)
                        }
                    }
                }
                BufferedOutputStream(FileOutputStream(candidate)).use { output ->
                    output.write("RIFF".toByteArray())
                    output.writeLittleUInt32(body.length() + 4)
                    output.write("WEBP".toByteArray())
                    FileInputStream(body).use { it.copyTo(output) }
                }
            } finally {
                body.delete()
            }
        }
    }

    private fun keepWebpChunk(name: String, mode: MetadataRemovalMode): Boolean = when (mode) {
        MetadataRemovalMode.LOCATION,
        MetadataRemovalMode.PRIVACY -> name != "XMP "
        MetadataRemovalMode.EVERYTHING -> name !in setOf("EXIF", "XMP ")
    }

    private fun rewriteGif(source: File, candidate: File, mode: MetadataRemovalMode) {
        if (mode == MetadataRemovalMode.LOCATION) {
            source.copyTo(candidate, overwrite = true)
            return
        }
        BufferedInputStream(FileInputStream(source)).use { input ->
            BufferedOutputStream(FileOutputStream(candidate)).use { output ->
                output.write(input.readExact(6))
                val logicalScreen = input.readExact(7)
                output.write(logicalScreen)
                if (logicalScreen[4].toInt() and 0x80 != 0) {
                    val tableSize = 3 * (1 shl ((logicalScreen[4].toInt() and 0x07) + 1))
                    output.write(input.readExact(tableSize))
                }
                while (true) {
                    when (val introducer = input.read()) {
                        -1 -> break
                        0x3B -> {
                            output.write(introducer)
                            break
                        }
                        0x2C -> {
                            output.write(introducer)
                            val descriptor = input.readExact(9)
                            output.write(descriptor)
                            if (descriptor[8].toInt() and 0x80 != 0) {
                                val tableSize = 3 * (1 shl ((descriptor[8].toInt() and 0x07) + 1))
                                output.write(input.readExact(tableSize))
                            }
                            output.write(input.read())
                            copySubBlocks(input, output)
                        }
                        0x21 -> {
                            val label = input.read()
                            require(label >= 0) { "Truncated GIF extension" }
                            if (label == 0xFE) {
                                skipSubBlocks(input)
                            } else {
                                output.write(introducer)
                                output.write(label)
                                copySubBlocks(input, output)
                            }
                        }
                        else -> error("Invalid GIF block: $introducer")
                    }
                }
            }
        }
    }

    private fun clearExifAttributes(file: File, mode: MetadataRemovalMode) {
        val exif = ExifInterface(file.absolutePath)
        val tags = when (mode) {
            MetadataRemovalMode.LOCATION -> locationTags
            MetadataRemovalMode.PRIVACY -> locationTags + privacyTags
            MetadataRemovalMode.EVERYTHING -> emptyArray()
        }
        tags.forEach { exif.setAttribute(it, null) }
        exif.saveAttributes()
    }

    private val locationTags = arrayOf(
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DIFFERENTIAL,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF
    )

    private val privacyTags = arrayOf(
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_MAKER_NOTE,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_USER_COMMENT
    )

    private fun copySubBlocks(input: InputStream, output: OutputStream) {
        while (true) {
            val size = input.read()
            require(size >= 0) { "Truncated data sub-block" }
            output.write(size)
            if (size == 0) break
            output.write(input.readExact(size))
        }
    }

    private fun skipSubBlocks(input: InputStream) {
        while (true) {
            val size = input.read()
            require(size >= 0) { "Truncated data sub-block" }
            if (size == 0) break
            input.readExact(size)
        }
    }

    private fun InputStream.readUnsignedShort(): Int {
        val high = read()
        val low = read()
        if (high < 0 || low < 0) throw EOFException()
        return high shl 8 or low
    }

    private fun OutputStream.writeUnsignedShort(value: Int) {
        write(value ushr 8)
        write(value)
    }

    private fun InputStream.readExact(size: Int): ByteArray = readExactOrNull(size) ?: throw EOFException()

    private fun InputStream.readExactOrNull(size: Int): ByteArray? {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = read(bytes, offset, size - offset)
            if (read < 0) return if (offset == 0) null else throw EOFException()
            offset += read
        }
        return bytes
    }

    private fun ByteArray.startsWithAscii(value: String): Boolean {
        val prefix = value.toByteArray(Charsets.ISO_8859_1)
        return size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
    }

    private fun ByteArray.readUInt32(offset: Int): Long =
        ((this[offset].toLong() and 0xFF) shl 24) or
            ((this[offset + 1].toLong() and 0xFF) shl 16) or
            ((this[offset + 2].toLong() and 0xFF) shl 8) or
            (this[offset + 3].toLong() and 0xFF)

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

    private fun OutputStream.writeUInt32(value: Long) {
        write((value ushr 24).toInt())
        write((value ushr 16).toInt())
        write((value ushr 8).toInt())
        write(value.toInt())
    }

    private fun OutputStream.writeLittleUInt32(value: Long) {
        write(value.toInt())
        write((value ushr 8).toInt())
        write((value ushr 16).toInt())
        write((value ushr 24).toInt())
    }
}
