/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.edit.bake

/**
 * Kotlin wrapper over the `heifenc` JNI library — libheif TILED (grid-image) HEIC/AVIF encode
 * backed by prebuilt static libheif (with x265 + aom encoders). Tiles are supplied as packed
 * ARGB_8888 ints (Bitmap.getPixels layout) so the tiled bake encodes a full-resolution image one
 * tile at a time without ever holding the whole output bitmap.
 *
 * [isAvailable] is false when the native library is a stub (ABI without prebuilt encoder libs);
 * callers then fall back to the whole-bitmap HeifCoder encode. Handles are NOT thread-safe.
 */
object NativeHeifEncoder {

    /** libheif compression target. */
    const val FORMAT_HEIC = 0
    const val FORMAT_AVIF = 1

    val isAvailable: Boolean by lazy {
        runCatching {
            System.loadLibrary("heifenc")
            nativeSelfTest()
        }.getOrDefault(false)
    }

    private external fun nativeSelfTest(): Boolean

    /** Opens a grid encoder writing to [fd]. Returns a handle, or 0 on failure. */
    external fun nativeOpen(
        fd: Int,
        width: Int,
        height: Int,
        tileWidth: Int,
        tileHeight: Int,
        format: Int,
        quality: Int,
    ): Long

    /** Encodes one [tileW]x[tileH] tile of packed-ARGB [argb] at grid position ([tileX],[tileY]). */
    external fun nativeEncodeTile(
        handle: Long,
        argb: IntArray,
        tileW: Int,
        tileH: Int,
        tileX: Int,
        tileY: Int,
    ): Boolean

    /** Writes the assembled HEIF/AVIF file and releases the encoder. */
    external fun nativeFinish(handle: Long): Boolean
}
