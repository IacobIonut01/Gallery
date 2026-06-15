/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Hardware-accelerated HEIC/AVIF decoding via the platform [ImageDecoder].
 *
 * Unlike the software [com.radzivon.bartoshyk.avif.coder.HeifCoder] (libheif/libavif), [ImageDecoder]
 * routes to the device's HEVC/AV1 hardware codec when available, which is dramatically faster.
 * Coverage varies by device/OS (HEIC works on API 28+, AVIF needs API 31+ and AV1 HW/SW support),
 * so callers must treat a null result as "unsupported here" and fall back to the software coder.
 *
 * The output is requested with [ImageDecoder.ALLOCATOR_SOFTWARE] so the result is a CPU-accessible
 * ARGB_8888 bitmap (required by Glide's centerCrop and Sketch transforms). The expensive entropy
 * decode still runs on the hardware codec regardless of the output allocator.
 */
object HardwareHeifDecoder {

    @RequiresApi(Build.VERSION_CODES.P)
    fun decode(bytes: ByteArray, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
                if (reqW > 0 && reqH > 0) {
                    val sw = info.size.width
                    val sh = info.size.height
                    if (sw > 0 && sh > 0) {
                        val scale = minOf(reqW.toFloat() / sw, reqH.toFloat() / sh)
                        if (scale < 1f) {
                            decoder.setTargetSize(
                                (sw * scale).roundToInt().coerceAtLeast(1),
                                (sh * scale).roundToInt().coerceAtLeast(1)
                            )
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }
}
