/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder.format

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.core.graphics.scale
import com.gemalto.jp2.JP2Decoder

/**
 * JPEG 2000 (.jp2 / .j2k) decoder backed by the OpenJPEG-based [JP2Decoder]. Android has no
 * native JPEG 2000 support. Uses the codec's multi-resolution feature ([JP2Decoder.setSkipResolutions])
 * to decode directly at a reduced resolution for fast thumbnails instead of decoding full-size
 * and downscaling afterwards.
 */
object Jp2ImageDecoder {

    private const val TAG = "Jp2ImageDecoder"

    fun getSize(bytes: ByteArray): Size? = runCatching {
        val header = JP2Decoder(bytes).readHeader() ?: return null
        Size(header.width, header.height)
    }.onFailure { Log.w(TAG, "readHeader failed: ${it.message}") }.getOrNull()

    /**
     * Decodes the JPEG 2000 image to a bitmap no larger than [reqW] x [reqH] (when > 0).
     */
    fun decode(bytes: ByteArray, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val header = JP2Decoder(bytes).readHeader()
            if (header == null) {
                JP2Decoder(bytes).decode()
            } else {
                val skip = computeSkipResolutions(
                    srcW = header.width,
                    srcH = header.height,
                    reqW = reqW,
                    reqH = reqH,
                    numResolutions = header.numResolutions
                )
                val bmp = JP2Decoder(bytes).setSkipResolutions(skip).decode() ?: return null
                if (reqW > 0 && reqH > 0) fit(bmp, reqW, reqH) else bmp
            }
        } catch (e: Throwable) {
            Log.e(TAG, "decode failed: ${e.message}", e)
            null
        }
    }

    private fun computeSkipResolutions(
        srcW: Int, srcH: Int, reqW: Int, reqH: Int, numResolutions: Int
    ): Int {
        if (reqW <= 0 || reqH <= 0 || srcW <= 0 || srcH <= 0) return 0
        var skip = 0
        var w = srcW
        var h = srcH
        val maxSkip = (numResolutions - 1).coerceAtLeast(0)
        // Halve while the next halving still covers the requested size.
        while (skip < maxSkip && w / 2 >= reqW && h / 2 >= reqH) {
            w /= 2
            h /= 2
            skip++
        }
        return skip
    }

    private fun fit(src: Bitmap, maxW: Int, maxH: Int): Bitmap {
        if (src.width <= maxW && src.height <= maxH) return src
        val scale = minOf(maxW.toFloat() / src.width, maxH.toFloat() / src.height)
        val tw = (src.width * scale).toInt().coerceAtLeast(1)
        val th = (src.height * scale).toInt().coerceAtLeast(1)
        val scaled = src.scale(tw, th)
        if (scaled != src) src.recycle()
        return scaled
    }
}
