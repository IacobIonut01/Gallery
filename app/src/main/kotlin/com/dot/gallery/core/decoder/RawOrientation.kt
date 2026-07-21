/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream

/**
 * Single source of truth for RAW display orientation. Some RAW containers (notably certain DNGs)
 * carry a correct EXIF/TIFF `Orientation` tag while LibRaw's parsed `flip` is `0`, and the embedded
 * JPEG preview / native thumbnail are stored un-rotated — leaving those paths rotated 90° off.
 *
 * We resolve orientation once from the container's EXIF tag and apply it uniformly:
 * - LibRaw paths (demosaic / subsampling / TIFF export) receive [libRawUserFlip] so LibRaw itself
 *   rotates the output consistently (overriding its possibly-wrong parsed flip).
 * - Kotlin-decoded bitmaps (embedded preview, native thumbnail) are rotated via [applyToBitmap].
 */
object RawOrientation {

    /** Reads the container EXIF orientation, defaulting to [ExifInterface.ORIENTATION_NORMAL]. */
    fun exifOrientation(data: ByteArray): Int = runCatching {
        ByteArrayInputStream(data).use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    /**
     * Maps an EXIF orientation to LibRaw's `user_flip` (0=none, 3=180°, 5=90° CCW, 6=90° CW).
     * Mirrored EXIF variants are approximated to their nearest pure rotation (rare in RAW).
     * Returns `-1` for normal/undefined so LibRaw keeps its parsed orientation only when there is
     * genuinely nothing to override — here we always return an explicit flip (0 for normal) so the
     * parsed value can never reintroduce the bug.
     */
    fun libRawUserFlip(data: ByteArray): Int = when (exifOrientation(data)) {
        ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> 3
        ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 6
        ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 5
        else -> 0 // ORIENTATION_NORMAL / FLIP_HORIZONTAL / UNDEFINED
    }

    /** True when the orientation swaps width and height (90° / 270°). */
    fun swapsDimensions(orientation: Int): Boolean = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE -> true
        else -> false
    }

    /** Rotates/mirrors [bitmap] to match [orientation]; returns the input unchanged when normal. */
    fun applyToBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }
}
