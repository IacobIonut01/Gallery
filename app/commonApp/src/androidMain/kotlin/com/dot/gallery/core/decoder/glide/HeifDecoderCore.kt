package com.dot.gallery.core.decoder.glide

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.dot.gallery.core.decoder.format.HeifDecodeEngine

/**
 * Shared core for HEIF/AVIF decoding, delegating to the unified [HeifDecodeEngine] (hardware-first
 * via the platform ImageDecoder, software fallback via the native libheif coder). Centralizes
 * target-dimension resolution and Glide [BitmapResource] wrapping so individual Glide decoders only
 * map their model type to bytes + mime decision.
 *
 * The grid renders these bitmaps through Glide's centerCrop, so HDR is kept off (SDR-safe
 * RGBA_8888) to avoid HARDWARE/F16 color artifacts.
 */
internal class HeifDecoderCore(
    private val bitmapPool: BitmapPool,
    @Suppress("unused") private val tag: String = "HeifDecoderCore"
) {

    data class Result(val resource: BitmapResource?, val success: Boolean)

    fun decodeBytes(
        bytes: ByteArray,
        requestedW: Int,
        requestedH: Int,
        @Suppress("UNUSED_PARAMETER") mime: String?
    ): Result {
        val bitmap = HeifDecodeEngine.decode(bytes, requestedW, requestedH, allowHdr = false)
            ?: return Result(null, false)
        return Result(BitmapResource.obtain(bitmap, bitmapPool), true)
    }
}
