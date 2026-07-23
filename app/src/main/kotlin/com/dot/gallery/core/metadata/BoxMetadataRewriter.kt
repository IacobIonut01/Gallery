package com.dot.gallery.core.metadata

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

internal object BoxMetadataRewriter {
    private val metadataTypes = setOf("xml ", "uuid", "Exif", "jumb")
    private val essenceTypes = setOf("jp2c", "jxlc", "jxlp")

    fun rewriteEverything(source: File, candidate: File) {
        BufferedInputStream(FileInputStream(source)).use { input ->
            BufferedOutputStream(FileOutputStream(candidate)).use { output ->
                while (true) {
                    val box = readHeader(input) ?: break
                    val metadata = box.type in metadataTypes || box.wrappedType in metadataTypes
                    if (metadata) {
                        input.skipExact(box.remainingData)
                    } else {
                        output.write(box.header)
                        input.copyExact(output, box.remainingData)
                    }
                }
            }
        }
    }

    fun essenceFingerprint(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(FileInputStream(file)).use { input ->
            while (true) {
                val box = readHeader(input) ?: break
                if (box.type in essenceTypes) {
                    digest.update(box.type.toByteArray(Charsets.ISO_8859_1))
                    input.digestExact(digest, box.remainingData)
                } else {
                    input.skipExact(box.remainingData)
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun hasEssence(file: File): Boolean = BufferedInputStream(FileInputStream(file)).use { input ->
        while (true) {
            val box = readHeader(input) ?: return@use false
            if (box.type in essenceTypes) return@use true
            input.skipExact(box.remainingData)
        }
        @Suppress("UNREACHABLE_CODE")
        false
    }

    fun containsMetadata(file: File): Boolean = BufferedInputStream(FileInputStream(file)).use { input ->
        while (true) {
            val box = readHeader(input) ?: return@use false
            if (box.type in metadataTypes || box.wrappedType in metadataTypes) return@use true
            input.skipExact(box.remainingData)
        }
        @Suppress("UNREACHABLE_CODE")
        false
    }

    private fun readHeader(input: InputStream): BoxHeader? {
        val sizeBytes = input.readExactOrNull(4) ?: return null
        val shortSize = sizeBytes.toUInt32()
        val typeBytes = input.readExact(4)
        val type = typeBytes.toString(Charsets.ISO_8859_1)
        val header = ByteArrayOutputStream().apply {
            write(sizeBytes)
            write(typeBytes)
        }
        val headerSize: Long
        val size: Long
        when (shortSize) {
            0L -> throw IllegalArgumentException("Unbounded $type boxes are not safely rewritable")
            1L -> {
                val extended = input.readExact(8)
                header.write(extended)
                size = extended.toUInt64()
                headerSize = 16
            }
            else -> {
                size = shortSize
                headerSize = 8
            }
        }
        require(size >= headerSize) { "Invalid $type box size" }
        var remaining = size - headerSize
        var wrappedType: String? = null
        if (type == "brob") {
            require(remaining >= 4) { "Invalid brob box" }
            val wrapped = input.readExact(4)
            header.write(wrapped)
            remaining -= 4
            wrappedType = wrapped.toString(Charsets.ISO_8859_1)
        }
        return BoxHeader(type, wrappedType, header.toByteArray(), remaining)
    }

    private data class BoxHeader(
        val type: String,
        val wrappedType: String?,
        val header: ByteArray,
        val remainingData: Long
    )

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

    private fun InputStream.copyExact(output: OutputStream, count: Long) {
        transferExact(count) { bytes, size -> output.write(bytes, 0, size) }
    }

    private fun InputStream.digestExact(digest: MessageDigest, count: Long) {
        transferExact(count) { bytes, size -> digest.update(bytes, 0, size) }
    }

    private fun InputStream.skipExact(count: Long) {
        transferExact(count) { _, _ -> }
    }

    private inline fun InputStream.transferExact(count: Long, consume: (ByteArray, Int) -> Unit) {
        var remaining = count
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (remaining > 0) {
            val read = read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw EOFException()
            consume(buffer, read)
            remaining -= read
        }
    }

    private fun ByteArray.toUInt32(): Long =
        ((this[0].toLong() and 0xFF) shl 24) or
            ((this[1].toLong() and 0xFF) shl 16) or
            ((this[2].toLong() and 0xFF) shl 8) or
            (this[3].toLong() and 0xFF)

    private fun ByteArray.toUInt64(): Long {
        require(this[0].toInt() and 0x80 == 0) { "Box size exceeds signed 64-bit range" }
        var value = 0L
        forEach { value = (value shl 8) or (it.toLong() and 0xFF) }
        return value
    }
}
