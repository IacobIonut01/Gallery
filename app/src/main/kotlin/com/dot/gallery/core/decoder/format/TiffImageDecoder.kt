/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

/**
 * TIFF decoder relying on the platform [ImageDecoder] (API 28+), which supports baseline TIFF on
 * most devices. Falls back to an embedded EXIF thumbnail for BigTIFF / oversized / unsupported
 * files. Returns null when nothing can be decoded so the caller can fall through.
 */
object TiffImageDecoder {

    private const val TAG = "TiffImageDecoder"
    private const val MAX_BYTES = 64 * 1024 * 1024

    fun getSize(bytes: ByteArray): Size? {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            if (w > 0 && h > 0) Size(w, h) else null
        } catch (_: Throwable) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun decode(bytes: ByteArray, reqW: Int, reqH: Int): Bitmap? {
        if (bytes.size > MAX_BYTES) {
            Log.w(TAG, "data too large=${bytes.size}; trying EXIF thumbnail")
            return exifThumbnail(bytes)
        }
        if (isBigTiff(bytes)) {
            Log.w(TAG, "BigTIFF unsupported by ImageDecoder; trying EXIF thumbnail")
            return exifThumbnail(bytes)
        }
        return try {
            val src = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                if (reqW > 0 && reqH > 0) decoder.setTargetSize(reqW, reqH)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "ImageDecoder failed: ${e.message}; trying EXIF thumbnail")
            exifThumbnail(bytes)
        }
    }

    private fun isBigTiff(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF
        val little = b0 == 0x49 && b1 == 0x49
        val big = b0 == 0x4D && b1 == 0x4D
        if (!little && !big) return false
        return (little && b2 == 0x2B && b3 == 0x00) || (big && b2 == 0x00 && b3 == 0x2B)
    }

    private fun exifThumbnail(bytes: ByteArray): Bitmap? {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            if (exif.hasThumbnail()) exif.thumbnailBitmap else null
        } catch (e: Throwable) {
            Log.e(TAG, "EXIF thumbnail extraction failed: ${e.message}")
            null
        }
    }
}
