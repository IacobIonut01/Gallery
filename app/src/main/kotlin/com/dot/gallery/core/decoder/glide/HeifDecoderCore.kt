package com.dot.gallery.core.decoder.glide

import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.dot.gallery.core.decoder.format.HardwareHeifDecoder
import com.radzivon.bartoshyk.avif.coder.HeifCoder
import com.radzivon.bartoshyk.avif.coder.PreferredColorConfig

/**
 * Shared core for HEIF/AVIF decoding using [HeifCoder]. Centralizes size fetch, target dimension
 * resolution, logging and error handling so individual Glide decoders only map their model type
 * to bytes + mime decision.
 */
internal class HeifDecoderCore(
    private val bitmapPool: BitmapPool,
    private val tag: String = "HeifDecoderCore"
) {
    private val coder = HeifCoder()

    data class Result(val resource: BitmapResource?, val success: Boolean)

    fun decodeBytes(
        bytes: ByteArray,
        requestedW: Int,
        requestedH: Int,
        mime: String?
    ): Result {
        // Hardware-first: ImageDecoder routes HEIC/AVIF to the device HEVC/AV1 hardware codec.
        // Returns null when unsupported on this device/OS, in which case we fall back to HeifCoder.
        HardwareHeifDecoder.decode(bytes, requestedW, requestedH)?.let {
            return Result(BitmapResource.obtain(it, bitmapPool), true)
        }
        return try {
            val size = coder.getSize(bytes)
            if (size == null) {
                // Size could not be read; treat as unsupported silently (caller decides fallback)
                Result(null, false)
            } else {
                val tw = if (requestedW > 0) requestedW else size.width
                val th = if (requestedH > 0) requestedH else size.height
                // Force RGBA_8888: the default config can yield a HARDWARE/F16 bitmap that Glide's
                // centerCrop transformation corrupts, producing pink/white color artifacts in the grid.
                val bmp = coder.decodeSampled(bytes, tw, th, PreferredColorConfig.RGBA_8888)
                // Success: intentionally no verbose log to reduce noise.
                Result(BitmapResource.obtain(bmp, bitmapPool), true)
            }
        } catch (e: Throwable) {
            Log.e(tag, "decode failed mime=$mime: ${e.message}", e)
            Result(null, false)
        }
    }
}
