/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import android.graphics.Bitmap
import android.util.Size as AndroidSize
import com.awxkee.jxlcoder.JxlAnimatedImage
import com.awxkee.jxlcoder.JxlCoder
import com.awxkee.jxlcoder.PreferredColorConfig
import com.dot.gallery.core.decoder.SketchJxlDecoder.Factory.Companion.JXL_MIMETYPE
import com.github.panpf.zoomimage.subsampling.ImageInfo
import com.github.panpf.zoomimage.subsampling.ImageSource
import com.github.panpf.zoomimage.subsampling.RegionDecoder
import com.github.panpf.zoomimage.subsampling.SubsamplingImage
import com.github.panpf.zoomimage.subsampling.TileBitmap
import com.github.panpf.zoomimage.util.IntRectCompat
import okio.buffer

/**
 * A zoomimage [RegionDecoder] for JPEG XL images.
 *
 * Android's [android.graphics.BitmapRegionDecoder] cannot decode JXL, so without this decoder
 * JXL images fall back to the base (screen-resolution) painter and look blurry when zoomed.
 *
 * Since [JxlCoder] has no native region API, the full image is decoded once at original
 * resolution and shared (reference-counted) across the pooled decoder copies. Each tile request
 * crops the requested region from the shared bitmap and samples it down by [sampleSize].
 *
 * Animated JXL is intentionally rejected in [prepare] so playback is handled by the animated
 * base painter instead.
 */
class JxlRegionDecoder(
    override val subsamplingImage: SubsamplingImage,
    val imageSource: ImageSource,
    private val shared: SharedFullBitmap = SharedFullBitmap(imageSource),
) : RegionDecoder {

    override val imageInfo: ImageInfo by lazy {
        val size = JxlCoder.getSize(shared.bytes) ?: AndroidSize(0, 0)
        ImageInfo(size.width, size.height, JXL_MIMETYPE)
    }

    override fun prepare() {
        shared.acquire()
    }

    override fun decodeRegion(region: IntRectCompat, sampleSize: Int): TileBitmap {
        val full = shared.acquire()
        val left = region.left.coerceIn(0, full.width)
        val top = region.top.coerceIn(0, full.height)
        val right = region.right.coerceIn(left, full.width)
        val bottom = region.bottom.coerceIn(top, full.height)
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        val regionBitmap = Bitmap.createBitmap(full, left, top, width, height)
        if (sampleSize <= 1) return regionBitmap

        val scaledWidth = (width / sampleSize).coerceAtLeast(1)
        val scaledHeight = (height / sampleSize).coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(regionBitmap, scaledWidth, scaledHeight, true)
        if (scaled != regionBitmap) {
            regionBitmap.recycle()
        }
        return scaled
    }

    override fun copy(): RegionDecoder {
        return JxlRegionDecoder(
            subsamplingImage = subsamplingImage,
            imageSource = imageSource,
            shared = shared,
        )
    }

    override fun close() {
        shared.release()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as JxlRegionDecoder
        if (subsamplingImage != other.subsamplingImage) return false
        if (imageSource != other.imageSource) return false
        return true
    }

    override fun hashCode(): Int {
        var result = subsamplingImage.hashCode()
        result = 31 * result + imageSource.hashCode()
        return result
    }

    override fun toString(): String {
        return "JxlRegionDecoder(subsamplingImage=$subsamplingImage, imageSource=$imageSource)"
    }

    /**
     * Holds the lazily-decoded full-resolution bitmap shared across pooled decoder copies and
     * recycles it once every copy has been closed.
     */
    class SharedFullBitmap(private val imageSource: ImageSource) {

        val bytes: ByteArray by lazy {
            imageSource.openSource().buffer().use { it.readByteArray() }
        }

        private var fullBitmap: Bitmap? = null
        private var refCount = 0
        private val lock = Any()

        fun acquire(): Bitmap = synchronized(lock) {
            fullBitmap?.let {
                refCount++
                return it
            }

            // Reject animated JXL so the animated base painter handles playback instead.
            val animated = JxlAnimatedImage(bytes)
            val frameCount = animated.numberOfFrames
            animated.close()
            check(frameCount <= 1) { "Animated JXL is not supported for subsampling" }

            val size = JxlCoder.getSize(bytes)
                ?: throw IllegalStateException("Invalid JXL: unable to read size")
            val bitmap = JxlCoder.decodeSampled(
                byteArray = bytes,
                width = size.width,
                height = size.height,
                preferredColorConfig = PreferredColorConfig.RGBA_8888,
            )
            fullBitmap = bitmap
            refCount++
            bitmap
        }

        fun release() = synchronized(lock) {
            refCount--
            if (refCount <= 0) {
                fullBitmap?.recycle()
                fullBitmap = null
                refCount = 0
            }
        }
    }

    class Factory : RegionDecoder.Factory {

        override suspend fun accept(subsamplingImage: SubsamplingImage): Boolean = true

        override fun checkSupport(mimeType: String): Boolean? = when (mimeType) {
            JXL_MIMETYPE -> true
            else -> null
        }

        override fun create(
            subsamplingImage: SubsamplingImage,
            imageSource: ImageSource,
        ): JxlRegionDecoder = JxlRegionDecoder(
            subsamplingImage = subsamplingImage,
            imageSource = imageSource,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other != null && this::class == other::class
        }

        override fun hashCode(): Int {
            return this::class.hashCode()
        }

        override fun toString(): String {
            return "JxlRegionDecoder"
        }
    }
}
