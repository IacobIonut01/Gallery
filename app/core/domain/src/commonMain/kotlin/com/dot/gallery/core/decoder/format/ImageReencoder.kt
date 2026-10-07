/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format


/**
 * Shared half of the re-encode surface: the output formats, quality config and
 * MIME/extension → format resolution. The actual bitmap encoders live in
 * `ImageReencoderCodecs` (androidMain — platform `Bitmap.compress` + native
 * JXL/AVIF/HEIC coders).
 *
 * Single source of truth for **re-encoding** an edited/rotated bitmap back into
 * the *same* image format as its source, so an overwrite (or a matching new
 * copy) never silently degrades e.g. a JXL/AVIF/HEIC image into JPEG or PNG.
 * Formats with no Android encoder (RAW/DNG, TIFF, PSD, JP2, SVG, animated
 * GIF/WebP) are reported as **not re-encodable** via [formatForMime] returning
 * `null`, so callers can offer a copy-as-PNG fallback instead.
 */
object ImageReencoder {

    /** A concrete output format the app is able to write bitmap pixels into. */
    enum class ImageWriteFormat(val mimeType: String, val fileExtension: String) {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP_LOSSLESS("image/webp", "webp"),
        WEBP_LOSSY("image/webp", "webp"),
        JXL("image/jxl", "jxl"),
        AVIF("image/avif", "avif"),
        HEIC("image/heic", "heic");

        val isNextGen: Boolean get() = this == JXL || this == AVIF || this == HEIC
    }

    /** How re-encode quality is resolved for lossy/lossless formats. */
    enum class QualityMode { AUTO, MANUAL }

    /**
     * Resolved encode parameters. [detectedQuality] (when non-null) is a best-effort estimate of
     * the source's original quality; it overrides the configured lossy quality in [QualityMode.AUTO].
     */
    data class ReencodeConfig(
        val mode: QualityMode = QualityMode.AUTO,
        val lossyQuality: Int = 90,
        val jxlEffort: Int = 7,
        val jxlLossless: Boolean = true,
        val detectedQuality: Int? = null,
    ) {
        /** Effective lossy quality (1..100) for JPEG/WebP/AVIF/HEIC. */
        val effectiveLossyQuality: Int
            get() = when (mode) {
                QualityMode.MANUAL -> lossyQuality
                QualityMode.AUTO -> (detectedQuality ?: lossyQuality)
            }.coerceIn(1, 100)

        /** Whether JXL should be written losslessly (AUTO with lossless toggle, and no lossy hint). */
        val jxlUseLossless: Boolean
            get() = when (mode) {
                QualityMode.MANUAL -> false
                QualityMode.AUTO -> jxlLossless && detectedQuality == null
            }
    }

    /**
     * Resolves the write format for a source [mime]/[label], or `null` when the source format has
     * no Android encoder (RAW, TIFF, PSD, JP2, SVG, animated GIF/WebP) and therefore cannot be
     * overwritten in place.
     */
    fun formatForMime(mime: String?, label: String?): ImageWriteFormat? {
        val m = mime?.lowercase().orEmpty()
        val ext = label?.substringAfterLast('.', "")?.lowercase().orEmpty()

        fun any(vararg tokens: String) = tokens.any { m.contains(it) || ext == it }

        return when {
            // Animated / no-encoder formats: cannot re-encode a single bitmap losslessly.
            m == "image/gif" || ext == "gif" -> null
            m == "image/apng" || ext == "apng" -> null
            any("svg") -> null
            any("tiff", "tif") -> null
            m.contains("photoshop") || any("psd", "psb") -> null
            m.contains("jp2") || m.contains("jpeg2000") || m.contains("jpx") ||
                ext in JP2_EXTENSIONS -> null
            isRawMime(m) || ext in RAW_EXTENSIONS -> null

            // Re-encodable formats.
            any("jxl") -> ImageWriteFormat.JXL
            m == "image/avif" || m == "image/avis" || ext == "avif" -> ImageWriteFormat.AVIF
            m.contains("heic") || m.contains("heif") || ext in HEIF_EXTENSIONS -> ImageWriteFormat.HEIC
            m.contains("png") || ext == "png" -> ImageWriteFormat.PNG
            m.contains("webp") || ext == "webp" -> ImageWriteFormat.WEBP_LOSSLESS
            m.contains("jpeg") || m.contains("jpg") || ext in JPEG_EXTENSIONS -> ImageWriteFormat.JPEG
            else -> null
        }
    }

    /** True if a source can be overwritten in place (i.e. an encoder exists). */
    fun isReencodable(mime: String?, label: String?): Boolean = formatForMime(mime, label) != null

    private fun isRawMime(m: String): Boolean =
        m.startsWith("image/x-") || m.startsWith("image/vnd.")

    private val JPEG_EXTENSIONS = setOf("jpg", "jpeg", "jpe", "jfif")
    private val HEIF_EXTENSIONS = setOf("heic", "heif", "hif")
    private val JP2_EXTENSIONS = setOf("jp2", "j2k", "jpf", "jpx", "j2c", "jpc")
    private val RAW_EXTENSIONS = setOf(
        "dng", "cr2", "cr3", "nef", "nrw", "arw", "rw2", "orf", "raf", "pef", "srw",
        "raw", "3fr", "fff", "dcr", "kdc", "mef", "mos", "iiq", "sr2", "srf"
    )
}
