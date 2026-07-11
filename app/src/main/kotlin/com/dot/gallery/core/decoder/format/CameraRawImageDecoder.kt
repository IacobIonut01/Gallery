/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

/**
 * Decoder for camera RAW containers (ARW, CR2, NEF, etc.). Android cannot demosaic proprietary
 * sensor data, so this extracts the embedded JPEG preview/thumbnail that manufacturers embed in
 * the TIFF-based container.
 */
object CameraRawImageDecoder {

    private const val TAG = "CameraRawImageDecoder"
    private const val MIN_JPEG_BYTES = 256

    private data class JpegSegment(val offset: Int, val length: Int)

    fun getSize(bytes: ByteArray, mimeType: String? = null): Size? {
        selectEmbeddedJpeg(bytes, reqW = 0, reqH = 0)?.let { segment ->
            jpegBounds(bytes, segment)?.let { return it }
        }
        return exifDimensions(bytes)
    }

    fun decode(bytes: ByteArray, reqW: Int, reqH: Int, mimeType: String? = null): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && CameraRawMime.isDng(mimeType)) {
            decodeDng(bytes, reqW, reqH)?.let { return it }
        }

        val targetW = if (reqW > 0) reqW else 0
        val targetH = if (reqH > 0) reqH else 0
        val wantsThumbnail = targetW > 0 && targetH > 0

        val exifThumb = if (wantsThumbnail) exifThumbnail(bytes) else null
        if (exifThumb != null && meetsTarget(exifThumb, targetW, targetH)) {
            return fit(exifThumb, targetW, targetH)
        }

        val embedded = selectEmbeddedJpeg(bytes, targetW, targetH)?.let { decodeJpeg(bytes, it) }
        val bitmap = embedded ?: exifThumbnail(bytes) ?: return null
        return fit(bitmap, targetW, targetH)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeDng(bytes: ByteArray, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val src = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                if (reqW > 0 && reqH > 0) decoder.setTargetSize(reqW, reqH)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "DNG ImageDecoder failed: ${e.message}")
            null
        }
    }

    private fun exifDimensions(bytes: ByteArray): Size? {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            if (w > 0 && h > 0) Size(w, h) else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun exifThumbnail(bytes: ByteArray): Bitmap? {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            if (exif.hasThumbnail()) exif.thumbnailBitmap else null
        } catch (e: Throwable) {
            Log.w(TAG, "EXIF thumbnail extraction failed: ${e.message}")
            null
        }
    }

    private fun selectEmbeddedJpeg(bytes: ByteArray, reqW: Int, reqH: Int): JpegSegment? {
        val segments = findJpegSegments(bytes)
        if (segments.isEmpty()) return null

        val sorted = segments.sortedByDescending { it.length }
        if (reqW <= 0 && reqH <= 0) return sorted.first()

        val sized = sorted.mapNotNull { segment ->
            val bounds = jpegBounds(bytes, segment) ?: return@mapNotNull null
            segment to bounds
        }
        val suitable = sized
            .filter { (_, bounds) -> bounds.width >= reqW && bounds.height >= reqH }
            .minByOrNull { (_, bounds) -> bounds.width.toLong() * bounds.height }
        return suitable?.first ?: sorted.first()
    }

    private fun findJpegSegments(bytes: ByteArray): List<JpegSegment> {
        val segments = ArrayList<JpegSegment>()
        var i = 0
        val limit = bytes.size - 1
        while (i < limit) {
            if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xD8.toByte()) {
                var j = i + 2
                var found = false
                while (j < limit) {
                    if (bytes[j] == 0xFF.toByte() && bytes[j + 1] == 0xD9.toByte()) {
                        val end = j + 2
                        val length = end - i
                        if (length >= MIN_JPEG_BYTES) {
                            segments.add(JpegSegment(i, length))
                        }
                        i = end
                        found = true
                        break
                    }
                    j++
                }
                if (!found) i++
            } else {
                i++
            }
        }
        return segments
    }

    private fun jpegBounds(bytes: ByteArray, segment: JpegSegment): Size? {
        if (segment.offset < 0 || segment.offset + segment.length > bytes.size) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, segment.offset, segment.length, options)
        return if (options.outWidth > 0 && options.outHeight > 0) {
            Size(options.outWidth, options.outHeight)
        } else {
            null
        }
    }

    private fun decodeJpeg(bytes: ByteArray, segment: JpegSegment): Bitmap? {
        if (segment.offset < 0 || segment.offset + segment.length > bytes.size) return null
        return BitmapFactory.decodeByteArray(bytes, segment.offset, segment.length)
    }

    private fun meetsTarget(bitmap: Bitmap, reqW: Int, reqH: Int): Boolean {
        if (reqW <= 0 && reqH <= 0) return true
        return bitmap.width >= reqW && bitmap.height >= reqH
    }

    private fun fit(src: Bitmap, maxW: Int, maxH: Int): Bitmap {
        if (maxW <= 0 || maxH <= 0) return src
        if (src.width <= maxW && src.height <= maxH) return src
        val scale = minOf(maxW.toFloat() / src.width, maxH.toFloat() / src.height)
        val tw = (src.width * scale).toInt().coerceAtLeast(1)
        val th = (src.height * scale).toInt().coerceAtLeast(1)
        val scaled = src.scale(tw, th)
        if (scaled != src) src.recycle()
        return scaled
    }
}
