/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format

/**
 * Lightweight magic-byte detection for image formats that Android cannot decode natively
 * and whose MIME types are unreliable (often null / application/octet-stream coming from
 * MediaStore or MimeTypeMap). Detection is done on the file header so it works regardless
 * of the reported MIME type.
 */
object ImageFormatSniffer {

    /** Adobe Photoshop document: starts with the ASCII signature "8BPS". */
    fun isPsd(header: ByteArray, length: Int = header.size): Boolean {
        if (length < 4) return false
        return header[0] == 0x38.toByte() && // '8'
                header[1] == 0x42.toByte() && // 'B'
                header[2] == 0x50.toByte() && // 'P'
                header[3] == 0x53.toByte()    // 'S'
    }

    /**
     * JPEG 2000: either the JP2/JPX file-format signature box
     * (00 00 00 0C 6A 50 20 20 0D 0A 87 0A) or a raw J2K codestream (FF 4F FF 51).
     */
    fun isJp2(header: ByteArray, length: Int = header.size): Boolean {
        if (length >= 12 &&
            header[0] == 0x00.toByte() && header[1] == 0x00.toByte() &&
            header[2] == 0x00.toByte() && header[3] == 0x0C.toByte() &&
            header[4] == 0x6A.toByte() && header[5] == 0x50.toByte() && // 'jP'
            header[6] == 0x20.toByte() && header[7] == 0x20.toByte() &&
            header[8] == 0x0D.toByte() && header[9] == 0x0A.toByte() &&
            header[10] == 0x87.toByte() && header[11] == 0x0A.toByte()
        ) return true
        // Raw J2K codestream
        return length >= 4 &&
                header[0] == 0xFF.toByte() && header[1] == 0x4F.toByte() &&
                header[2] == 0xFF.toByte() && header[3] == 0x51.toByte()
    }

    /**
     * SVG: an XML/text document containing a "<svg" tag near the start. Tolerates a UTF-8 BOM,
     * leading whitespace, and an optional XML prolog / DOCTYPE before the root element.
     */
    fun isSvg(header: ByteArray, length: Int = header.size): Boolean {
        if (length < 4) return false
        var i = 0
        // Skip UTF-8 BOM
        if (length >= 3 &&
            header[0] == 0xEF.toByte() && header[1] == 0xBB.toByte() && header[2] == 0xBF.toByte()
        ) i = 3
        // Skip leading whitespace
        while (i < length) {
            val c = header[i].toInt()
            if (c != 0x20 && c != 0x09 && c != 0x0A && c != 0x0D) break
            i++
        }
        // Must start with a tag (XML prolog, DOCTYPE, comment, or the <svg> root)
        if (i >= length || header[i] != '<'.code.toByte()) return false
        // Cheap byte scan for "<svg" anywhere in the sniffed header
        val end = length - 4
        var j = i
        while (j <= end) {
            if (header[j] == '<'.code.toByte() &&
                (header[j + 1] == 's'.code.toByte() || header[j + 1] == 'S'.code.toByte()) &&
                (header[j + 2] == 'v'.code.toByte() || header[j + 2] == 'V'.code.toByte()) &&
                (header[j + 3] == 'g'.code.toByte() || header[j + 3] == 'G'.code.toByte())
            ) return true
            j++
        }
        return false
    }
}
