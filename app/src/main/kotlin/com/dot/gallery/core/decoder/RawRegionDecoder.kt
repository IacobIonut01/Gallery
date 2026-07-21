/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import android.graphics.Bitmap
import com.github.panpf.zoomimage.subsampling.ImageInfo
import com.github.panpf.zoomimage.subsampling.ImageSource
import com.github.panpf.zoomimage.subsampling.RegionDecoder
import com.github.panpf.zoomimage.subsampling.SubsamplingImage
import com.github.panpf.zoomimage.subsampling.TileBitmap
import com.github.panpf.zoomimage.util.IntRectCompat
import okio.buffer

/**
 * A zoomimage [RegionDecoder] for camera-RAW files, backed by [NativeRawDecoder] (LibRaw). Android's
 * [android.graphics.BitmapRegionDecoder] can't decode RAW, and LibRaw has no native region API, so
 * the RAW is demosaiced **once** into a shared full (or auto half-size) bitmap and each tile request
 * crops + samples from it — the same proven approach as [FullImageRegionDecoder], but the reported
 * [ImageInfo] reflects the actual demosaiced buffer (which may be half-size) so tile coordinates map
 * correctly.
 *
 * Only usable when [NativeRawDecoder.isAvailable]; otherwise callers keep the embedded-preview path.
 */
class RawRegionDecoder(
    val subsamplingImage: SubsamplingImage,
    val imageSource: ImageSource,
    private val params: RawDevelopParams,
    private val shared: SharedRawBitmap = SharedRawBitmap(imageSource, params),
) : RegionDecoder {

    private val cachedImageInfo: ImageInfo by lazy {
        val size = shared.size()
        ImageInfo(size?.first ?: 0, size?.second ?: 0, RAW_MIMETYPE)
    }

    override fun getImageInfo(): ImageInfo = cachedImageInfo

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
        if (scaled != regionBitmap) regionBitmap.recycle()
        return scaled
    }

    override fun copy(): RegionDecoder = RawRegionDecoder(
        subsamplingImage = subsamplingImage,
        imageSource = imageSource,
        params = params,
        shared = shared,
    )

    override fun close() {
        shared.release()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as RawRegionDecoder
        return subsamplingImage == other.subsamplingImage &&
            imageSource == other.imageSource &&
            params == other.params
    }

    override fun hashCode(): Int {
        var result = subsamplingImage.hashCode()
        result = 31 * result + imageSource.hashCode()
        result = 31 * result + params.hashCode()
        return result
    }

    override fun toString(): String = "RawRegionDecoder(params=$params)"

    /**
     * Demosaics the RAW once and shares the resulting bitmap (reference-counted) across pooled
     * decoder copies, recycling it after every copy is closed. Applies an automatic half-size when
     * the sensor is very large, unless the caller already requested it.
     */
    class SharedRawBitmap(
        private val imageSource: ImageSource,
        private val params: RawDevelopParams,
    ) {
        val bytes: ByteArray by lazy {
            imageSource.openSource().buffer().use { it.readByteArray() }
        }

        private val resolvedParams: RawDevelopParams by lazy {
            val size = NativeRawDecoder.getSize(bytes)
            val large = size != null &&
                size.width.toLong() * size.height.toLong() > AUTO_HALFSIZE_PIXELS
            if (large && !params.halfSize) params.copy(halfSize = true) else params
        }

        /** EXIF-derived orientation (single source of truth), applied by LibRaw during demosaic. */
        private val userFlip: Int by lazy { RawOrientation.libRawUserFlip(bytes) }

        private var fullBitmap: Bitmap? = null
        private var refCount = 0
        private val lock = Any()

        private fun decodeLocked(): Bitmap {
            fullBitmap?.let { return it }
            val bitmap = NativeRawDecoder.demosaic(bytes, resolvedParams, userFlip)
                ?: throw IllegalStateException("Unable to demosaic RAW for subsampling")
            fullBitmap = bitmap
            return bitmap
        }

        /** Decoded dimensions (decodes once if needed, keeps the bitmap for later tile requests). */
        fun size(): Pair<Int, Int>? = synchronized(lock) {
            runCatching { decodeLocked() }.getOrNull()?.let { it.width to it.height }
        }

        fun acquire(): Bitmap = synchronized(lock) {
            val bitmap = decodeLocked()
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

    class Factory(
        private val params: RawDevelopParams,
    ) : RegionDecoder.Factory {

        override suspend fun accept(subsamplingImage: SubsamplingImage): Boolean =
            NativeRawDecoder.isAvailable

        override fun checkSupport(mimeType: String): Boolean? =
            if (mimeType == RAW_MIMETYPE) true else null

        override suspend fun create(
            subsamplingImage: SubsamplingImage,
            imageSource: ImageSource,
        ): RawRegionDecoder = RawRegionDecoder(
            subsamplingImage = subsamplingImage,
            imageSource = imageSource,
            params = params,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Factory
            return params == other.params
        }

        override fun hashCode(): Int = params.hashCode()

        override fun toString(): String = "RawRegionDecoder.Factory(params=$params)"
    }

    companion object {
        const val RAW_MIMETYPE = "image/x-raw"

        /** Sensors above this pixel count are auto half-sized for the zoom buffer to bound memory. */
        const val AUTO_HALFSIZE_PIXELS = 40_000_000L

        fun forRaw(params: RawDevelopParams = RawDevelopParams.AUTO): Factory = Factory(params)
    }
}
