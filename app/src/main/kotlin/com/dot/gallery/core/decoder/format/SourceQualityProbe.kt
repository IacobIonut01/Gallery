/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format

/**
 * Best-effort estimation of a source image's original encode quality.
 *
 * There is no standard, portable "quality" tag in EXIF or in the JXL/AVIF/HEIC containers, so the
 * only format we can meaningfully probe is **JPEG**, whose quantization tables (DQT markers) encode
 * the quality the file was written at. This uses the classic IJG estimation: sum the luminance
 * quantization table and map it back to the 1..100 quality scale.
 *
 * Returns `null` when the quality cannot be determined (non-JPEG, corrupt, or no DQT found), in
 * which case callers fall back to the user's re-encode quality setting.
 */
object SourceQualityProbe {

    /** Standard IJG luminance quantization table (quality 50 baseline). */
    private val STD_LUMINANCE_QT = intArrayOf(
        16, 11, 10, 16, 24, 40, 51, 61,
        12, 12, 14, 19, 26, 58, 60, 55,
        14, 13, 16, 24, 40, 57, 69, 56,
        14, 17, 22, 29, 51, 87, 80, 62,
        18, 22, 37, 56, 68, 109, 103, 77,
        24, 35, 55, 64, 81, 104, 113, 92,
        49, 64, 78, 87, 103, 121, 120, 101,
        72, 92, 95, 98, 112, 100, 103, 99
    )

    /**
     * Estimates JPEG quality (1..100) from [bytes], or `null` if [mime] isn't JPEG / no table found.
     */
    fun detect(bytes: ByteArray, mime: String?): Int? {
        val m = mime?.lowercase().orEmpty()
        if (!(m.contains("jpeg") || m.contains("jpg"))) return null
        return estimateJpegQuality(bytes)
    }

    private fun estimateJpegQuality(bytes: ByteArray): Int? {
        val qt = firstLuminanceQuantTable(bytes) ?: return null
        // Sum of quantization coefficients; larger sum => lower quality.
        var qtSum = 0L
        for (v in qt) qtSum += v
        var stdSum = 0L
        for (v in STD_LUMINANCE_QT) stdSum += v
        if (qtSum <= 0L) return null

        // Ratio vs. the quality-50 standard table. IJG scaling:
        //   q < 50 -> scale = 5000 / quality ; q >= 50 -> scale = 200 - 2*quality
        // Invert using the observed scale factor (average across coefficients).
        val scale = (qtSum.toDouble() / stdSum.toDouble()) * 100.0
        val quality = if (scale <= 100.0) {
            (200.0 - scale) / 2.0
        } else {
            5000.0 / scale
        }
        return quality.toInt().coerceIn(1, 100)
    }

    /** Scans JPEG markers for the first DQT and returns its 64-entry luminance table. */
    private fun firstLuminanceQuantTable(bytes: ByteArray): IntArray? {
        if (bytes.size < 4) return null
        // SOI
        if (bytes[0].toInt() and 0xFF != 0xFF || bytes[1].toInt() and 0xFF != 0xD8) return null
        var i = 2
        while (i + 4 < bytes.size) {
            if (bytes[i].toInt() and 0xFF != 0xFF) { i++; continue }
            val marker = bytes[i + 1].toInt() and 0xFF
            // Standalone markers without length.
            if (marker == 0xD9 || marker == 0x01 || marker in 0xD0..0xD7) { i += 2; continue }
            val len = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
            if (len < 2) return null
            if (marker == 0xDB) { // DQT
                var p = i + 4
                val end = i + 2 + len
                while (p < end && p < bytes.size) {
                    val pqTq = bytes[p].toInt() and 0xFF
                    val precision = pqTq shr 4 // 0 = 8-bit, 1 = 16-bit
                    p++
                    val table = IntArray(64)
                    if (precision == 0) {
                        if (p + 64 > bytes.size) return null
                        for (k in 0 until 64) table[k] = bytes[p + k].toInt() and 0xFF
                        p += 64
                    } else {
                        if (p + 128 > bytes.size) return null
                        for (k in 0 until 64) {
                            table[k] = ((bytes[p + 2 * k].toInt() and 0xFF) shl 8) or
                                (bytes[p + 2 * k + 1].toInt() and 0xFF)
                        }
                        p += 128
                    }
                    // First table encountered is the luminance table.
                    return table
                }
            }
            // Start of scan: no more headers worth parsing.
            if (marker == 0xDA) return null
            i += 2 + len
        }
        return null
    }
}
