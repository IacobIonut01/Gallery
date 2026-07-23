package com.dot.gallery.core.metadata

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

internal object MediaEssenceFingerprint {
    fun calculate(file: File, format: MediaContainerFormat): String {
        val digest = MessageDigest.getInstance("SHA-256")
        when (format) {
            MediaContainerFormat.JPEG -> hashJpeg(file, digest)
            MediaContainerFormat.PNG -> hashPng(file, digest)
            MediaContainerFormat.WEBP -> hashWebp(file, digest)
            MediaContainerFormat.GIF -> hashGif(file, digest)
            MediaContainerFormat.BMP -> hashBmp(file, digest)
            MediaContainerFormat.JP2,
            MediaContainerFormat.JXL -> return BoxMetadataRewriter.essenceFingerprint(file)
            else -> error("No essence fingerprint for $format")
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun hashJpeg(file: File, digest: MessageDigest) {
        BufferedInputStream(FileInputStream(file)).use { input ->
            require(input.read() == 0xFF && input.read() == 0xD8)
            while (true) {
                require(input.read() == 0xFF)
                var marker = input.read()
                while (marker == 0xFF) marker = input.read()
                if (marker == 0xD9) return
                if (marker == 0xDA) {
                    val length = input.readUnsignedShort()
                    digest.update(input.readExact(length - 2))
                    input.copyToDigest(digest)
                    return
                }
                if (marker in 0xD0..0xD7 || marker == 0x01) continue
                val length = input.readUnsignedShort()
                val data = input.readExact(length - 2)
                if (marker !in setOf(0xE1, 0xEB, 0xED, 0xFE)) {
                    digest.update(marker.toByte())
                    digest.update(data)
                }
            }
        }
    }

    private fun hashPng(file: File, digest: MessageDigest) {
        BufferedInputStream(FileInputStream(file)).use { input ->
            input.readExact(8)
            while (true) {
                val lengthBytes = input.readExactOrNull(4) ?: break
                val length = lengthBytes.toUInt32()
                require(length <= Int.MAX_VALUE.toLong())
                val type = input.readExact(4)
                val data = input.readExact(length.toInt())
                input.readExact(4)
                val name = type.toString(Charsets.ISO_8859_1)
                if (name in setOf("IHDR", "PLTE", "tRNS", "IDAT", "acTL", "fcTL", "fdAT", "IEND")) {
                    digest.update(type)
                    digest.update(data)
                }
                if (name == "IEND") break
            }
        }
    }

    private fun hashWebp(file: File, digest: MessageDigest) {
        BufferedInputStream(FileInputStream(file)).use { input ->
            input.readExact(12)
            while (true) {
                val type = input.readExactOrNull(4) ?: break
                val size = input.readExact(4).toLittleUInt32()
                require(size <= Int.MAX_VALUE.toLong())
                val data = input.readExact(size.toInt())
                if (size and 1L == 1L) input.read()
                val name = type.toString(Charsets.US_ASCII)
                if (name == "VP8X" && data.isNotEmpty()) {
                    data[0] = (data[0].toInt() and 0xF3).toByte()
                }
                if (name !in setOf("EXIF", "XMP ")) {
                    digest.update(type)
                    digest.update(data)
                }
            }
        }
    }

    private fun hashGif(file: File, digest: MessageDigest) {
        BufferedInputStream(FileInputStream(file)).use { input ->
            digest.update(input.readExact(6))
            val logicalScreen = input.readExact(7)
            digest.update(logicalScreen)
            if (logicalScreen[4].toInt() and 0x80 != 0) {
                val tableSize = 3 * (1 shl ((logicalScreen[4].toInt() and 0x07) + 1))
                digest.update(input.readExact(tableSize))
            }
            while (true) {
                when (val introducer = input.read()) {
                    -1 -> return
                    0x3B -> {
                        digest.update(introducer.toByte())
                        return
                    }
                    0x2C -> {
                        digest.update(introducer.toByte())
                        val descriptor = input.readExact(9)
                        digest.update(descriptor)
                        if (descriptor[8].toInt() and 0x80 != 0) {
                            val tableSize = 3 * (1 shl ((descriptor[8].toInt() and 0x07) + 1))
                            digest.update(input.readExact(tableSize))
                        }
                        digest.update(input.read().toByte())
                        hashGifSubBlocks(input, digest)
                    }
                    0x21 -> {
                        val label = input.read()
                        if (label == 0xFE) {
                            skipGifSubBlocks(input)
                        } else {
                            digest.update(introducer.toByte())
                            digest.update(label.toByte())
                            hashGifSubBlocks(input, digest)
                        }
                    }
                    else -> error("Invalid GIF block: $introducer")
                }
            }
        }
    }

    private fun hashGifSubBlocks(input: InputStream, digest: MessageDigest) {
        while (true) {
            val size = input.read()
            if (size < 0) throw EOFException()
            digest.update(size.toByte())
            if (size == 0) return
            digest.update(input.readExact(size))
        }
    }

    private fun skipGifSubBlocks(input: InputStream) {
        while (true) {
            val size = input.read()
            if (size < 0) throw EOFException()
            if (size == 0) return
            input.readExact(size)
        }
    }

    private fun hashBmp(file: File, digest: MessageDigest) {
        BufferedInputStream(FileInputStream(file)).use { input ->
            val header = input.readExact(14)
            val offset = (header[10].toInt() and 0xFF) or
                ((header[11].toInt() and 0xFF) shl 8) or
                ((header[12].toInt() and 0xFF) shl 16) or
                ((header[13].toInt() and 0xFF) shl 24)
            require(offset >= 14)
            input.skipExact((offset - 14).toLong())
            input.copyToDigest(digest)
        }
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

    private fun InputStream.skipExact(count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped <= 0) {
                if (read() < 0) throw EOFException()
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun InputStream.copyToDigest(digest: MessageDigest) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
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
