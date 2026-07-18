/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.edit.bake

/**
 * Kotlin wrapper over the `imgstream` JNI library — scanline (row-streaming) JPEG + PNG encoders
 * backed by prebuilt static libjpeg-turbo + libpng. Rows are supplied as packed ARGB_8888 ints
 * (Bitmap.getPixels layout) so the tiled bake can write a full-resolution image one strip at a time
 * without ever holding the whole output bitmap.
 *
 * [isAvailable] is false when the native library is a stub (ABI without prebuilt libs); callers
 * then fall back to the whole-bitmap encode. Handles are NOT thread-safe — serialise per encoder.
 */
object NativeImageEncoder {

    val isAvailable: Boolean by lazy {
        runCatching {
            System.loadLibrary("imgstream")
            nativeSelfTest()
        }.getOrDefault(false)
    }

    private external fun nativeSelfTest(): Boolean

    external fun nativeJpegOpen(fd: Int, width: Int, height: Int, quality: Int): Long
    external fun nativeJpegWriteRows(handle: Long, argb: IntArray, rowCount: Int): Boolean
    external fun nativeJpegFinish(handle: Long): Boolean

    external fun nativePngOpen(fd: Int, width: Int, height: Int): Long
    external fun nativePngWriteRows(handle: Long, argb: IntArray, rowCount: Int): Boolean
    external fun nativePngFinish(handle: Long): Boolean
}
