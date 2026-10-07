/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format

import android.graphics.Bitmap
import com.awxkee.jxlcoder.JxlChannelsConfiguration
import com.awxkee.jxlcoder.JxlCoder
import com.awxkee.jxlcoder.JxlCompressionOption
import com.awxkee.jxlcoder.JxlDecodingSpeed
import com.awxkee.jxlcoder.JxlEffort
import com.dot.gallery.core.decoder.format.ImageReencoder.ImageWriteFormat
import com.dot.gallery.core.decoder.format.ImageReencoder.QualityMode
import com.dot.gallery.core.decoder.format.ImageReencoder.ReencodeConfig
import com.dot.gallery.core.logging.printError
import com.radzivon.bartoshyk.avif.coder.HeifCoder
import com.radzivon.bartoshyk.avif.coder.HeifQualityArg
import com.radzivon.bartoshyk.avif.coder.PreciseMode
import java.io.OutputStream


/**
 * Android half of [ImageReencoder]: the actual bitmap encoders.
 *
 * JPEG/PNG/WebP use the platform [Bitmap.compress]. JXL/AVIF/HEIC use the
 * native prebuilt coders ([JxlCoder], [HeifCoder]) which return raw
 * `ByteArray`s.
 *
 * Every native call is guarded so a failure surfaces as an exception the caller
 * can catch and degrade from, rather than crashing.
 */
object ImageReencoderCodecs {

    private const val TAG = "decode.reencode"

    private val heifCoder by lazy { HeifCoder() }

    /**
     * Encodes [bitmap] into [format] and returns the raw bytes. Throws on failure.
     */
    fun encodeToBytes(
        bitmap: Bitmap,
        format: ImageWriteFormat,
        config: ReencodeConfig,
    ): ByteArray = when (format) {
        ImageWriteFormat.JPEG ->
            compressToBytes(bitmap, Bitmap.CompressFormat.JPEG, config.effectiveLossyQuality)

        ImageWriteFormat.PNG ->
            compressToBytes(bitmap, Bitmap.CompressFormat.PNG, 100)

        ImageWriteFormat.WEBP_LOSSLESS ->
            compressToBytes(bitmap, webpLossless(), 100)

        ImageWriteFormat.WEBP_LOSSY ->
            compressToBytes(bitmap, webpLossy(), config.effectiveLossyQuality)

        ImageWriteFormat.JXL -> encodeJxl(bitmap, config)
        ImageWriteFormat.AVIF -> encodeAvif(bitmap, config)
        ImageWriteFormat.HEIC -> encodeHeic(bitmap, config)
    }

    /** Encodes [bitmap] into [format] and writes it to [out]. Throws on failure. */
    fun writeToStream(
        bitmap: Bitmap,
        format: ImageWriteFormat,
        config: ReencodeConfig,
        out: OutputStream,
    ) {
        when (format) {
            ImageWriteFormat.JPEG ->
                compressToStream(bitmap, Bitmap.CompressFormat.JPEG, config.effectiveLossyQuality, out)

            ImageWriteFormat.PNG ->
                compressToStream(bitmap, Bitmap.CompressFormat.PNG, 100, out)

            ImageWriteFormat.WEBP_LOSSLESS ->
                compressToStream(bitmap, webpLossless(), 100, out)

            ImageWriteFormat.WEBP_LOSSY ->
                compressToStream(bitmap, webpLossy(), config.effectiveLossyQuality, out)

            ImageWriteFormat.JXL, ImageWriteFormat.AVIF, ImageWriteFormat.HEIC ->
                out.write(encodeToBytes(bitmap, format, config))
        }
    }

    // ---- native encoders ----

    private fun encodeJxl(bitmap: Bitmap, config: ReencodeConfig): ByteArray {
        val src = bitmap.toArgb8888()
        val channels =
            if (src.hasAlpha()) JxlChannelsConfiguration.RGBA else JxlChannelsConfiguration.RGB
        val lossless = config.jxlUseLossless
        val compression =
            if (lossless) JxlCompressionOption.LOSSLESS else JxlCompressionOption.LOSSY
        // libjxl quality: 100 = lossless; lower = lossy. AUTO-lossless pins 100.
        val quality = if (lossless) 100 else config.effectiveLossyQuality
        return try {
            JxlCoder.encode(
                bitmap = src,
                channelsConfiguration = channels,
                compressionOption = compression,
                effort = effortFor(config.jxlEffort),
                quality = quality,
                decodingSpeed = JxlDecodingSpeed.SLOWEST,
            )
        } finally {
            if (src !== bitmap) src.recycle()
        }
    }

    private fun encodeAvif(bitmap: Bitmap, config: ReencodeConfig): ByteArray {
        val src = bitmap.toArgb8888()
        return try {
            heifCoder.encodeAvif(src, config.effectiveLossyQuality)
        } finally {
            if (src !== bitmap) src.recycle()
        }
    }

    private fun encodeHeic(bitmap: Bitmap, config: ReencodeConfig): ByteArray {
        val src = bitmap.toArgb8888()
        return try {
            heifCoder.encodeHeic(
                src,
                PreciseMode.LOSSY,
                HeifQualityArg.Quality(config.effectiveLossyQuality),
            )
        } finally {
            if (src !== bitmap) src.recycle()
        }
    }

    private fun effortFor(effort: Int): JxlEffort {
        val entries = JxlEffort.entries
        val idx = (effort - 1).coerceIn(0, entries.size - 1)
        return entries[idx]
    }

    // ---- platform compress helpers ----

    private fun compressToBytes(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        quality: Int,
    ): ByteArray = java.io.ByteArrayOutputStream().use { bos ->
        if (!bitmap.compress(format, quality.coerceIn(0, 100), bos)) {
            error("Bitmap.compress($format) returned false")
        }
        bos.toByteArray()
    }

    private fun compressToStream(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        quality: Int,
        out: OutputStream,
    ) {
        if (!bitmap.compress(format, quality.coerceIn(0, 100), out)) {
            error("Bitmap.compress($format) returned false")
        }
    }

    @Suppress("DEPRECATION")
    private fun webpLossless(): Bitmap.CompressFormat =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
        } else {
            Bitmap.CompressFormat.WEBP
        }

    @Suppress("DEPRECATION")
    private fun webpLossy(): Bitmap.CompressFormat =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

    private fun Bitmap.toArgb8888(): Bitmap =
        if (config == Bitmap.Config.ARGB_8888) this
        else try {
            copy(Bitmap.Config.ARGB_8888, false) ?: this
        } catch (e: Throwable) {
            printError(TAG, "ARGB_8888 copy failed: ${e.message}")
            this
        }
}
