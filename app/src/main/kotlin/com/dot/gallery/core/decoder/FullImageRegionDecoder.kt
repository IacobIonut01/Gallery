/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import android.graphics.Bitmap
import android.util.Size as AndroidSize
import com.dot.gallery.core.decoder.format.Jp2ImageDecoder
import com.dot.gallery.core.decoder.format.PsdImageDecoder
import com.dot.gallery.core.decoder.format.SvgImageDecoder
import com.dot.gallery.core.decoder.format.TiffImageDecoder
import com.github.panpf.zoomimage.subsampling.ImageInfo
import com.github.panpf.zoomimage.subsampling.ImageSource
import com.github.panpf.zoomimage.subsampling.RegionDecoder
import com.github.panpf.zoomimage.subsampling.SubsamplingImage
import com.github.panpf.zoomimage.subsampling.TileBitmap
import com.github.panpf.zoomimage.util.IntRectCompat
import okio.buffer

/**
 * A generic zoomimage [RegionDecoder] for formats that Android's [android.graphics.BitmapRegionDecoder]
 * cannot subsample (PSD, JPEG 2000, TIFF, SVG). Without it these images fall back to the base
 * (screen-resolution) painter and look blurry when zoomed.
 *
 * None of these formats expose a native region API, so the full image is decoded once at base
 * resolution and shared (reference-counted) across the pooled decoder copies. Each tile request
 * crops the requested region from the shared bitmap and samples it down by `sampleSize` — the same
 * proven approach used by [JxlRegionDecoder].
 *
 * Configure one via the [forPsd]/[forJp2]/[forTiff]/[forSvg] factory builders.
 */
class FullImageRegionDecoder(
    override val subsamplingImage: SubsamplingImage,
    val imageSource: ImageSource,
    private val mimeType: String,
    private val decodeFull: (ByteArray) -> Bitmap?,
    private val sizeOf: (ByteArray) -> AndroidSize?,
    private val shared: SharedFullBitmap = SharedFullBitmap(imageSource, decodeFull),
) : RegionDecoder {

    override val imageInfo: ImageInfo by lazy {
        val size = sizeOf(shared.bytes) ?: AndroidSize(0, 0)
        ImageInfo(size.width, size.height, mimeType)
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

    override fun copy(): RegionDecoder = FullImageRegionDecoder(
        subsamplingImage = subsamplingImage,
        imageSource = imageSource,
        mimeType = mimeType,
        decodeFull = decodeFull,
        sizeOf = sizeOf,
        shared = shared,
    )

    override fun close() {
        shared.release()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as FullImageRegionDecoder
        if (subsamplingImage != other.subsamplingImage) return false
        if (imageSource != other.imageSource) return false
        if (mimeType != other.mimeType) return false
        return true
    }

    override fun hashCode(): Int {
        var result = subsamplingImage.hashCode()
        result = 31 * result + imageSource.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }

    override fun toString(): String =
        "FullImageRegionDecoder(mimeType=$mimeType, subsamplingImage=$subsamplingImage)"

    /**
     * Holds the lazily-decoded full-resolution bitmap shared across pooled decoder copies and
     * recycles it once every copy has been closed.
     */
    class SharedFullBitmap(
        private val imageSource: ImageSource,
        private val decodeFull: (ByteArray) -> Bitmap?,
    ) {
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
            val bitmap = decodeFull(bytes)
                ?: throw IllegalStateException("Unable to decode image for subsampling")
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

    class Factory(
        private val mimeType: String,
        private val supportedMimeTypes: Set<String>,
        private val decodeFull: (ByteArray) -> Bitmap?,
        private val sizeOf: (ByteArray) -> AndroidSize?,
    ) : RegionDecoder.Factory {

        override suspend fun accept(subsamplingImage: SubsamplingImage): Boolean = true

        override fun checkSupport(mimeType: String): Boolean? =
            if (mimeType in supportedMimeTypes) true else null

        override fun create(
            subsamplingImage: SubsamplingImage,
            imageSource: ImageSource,
        ): FullImageRegionDecoder = FullImageRegionDecoder(
            subsamplingImage = subsamplingImage,
            imageSource = imageSource,
            mimeType = mimeType,
            decodeFull = decodeFull,
            sizeOf = sizeOf,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Factory
            return mimeType == other.mimeType
        }

        override fun hashCode(): Int = mimeType.hashCode()

        override fun toString(): String = "FullImageRegionDecoder.Factory($mimeType)"
    }

    companion object {
        const val PSD_MIMETYPE = "image/vnd.adobe.photoshop"
        const val JP2_MIMETYPE = "image/jp2"
        const val TIFF_MIMETYPE = "image/tiff"
        const val SVG_MIMETYPE = "image/svg+xml"

        fun forPsd(): Factory = Factory(
            mimeType = PSD_MIMETYPE,
            supportedMimeTypes = setOf(PSD_MIMETYPE, "image/x-photoshop", "image/photoshop"),
            decodeFull = { PsdImageDecoder.decode(it, 0, 0) },
            sizeOf = { PsdImageDecoder.getSize(it) },
        )

        fun forJp2(): Factory = Factory(
            mimeType = JP2_MIMETYPE,
            supportedMimeTypes = setOf(JP2_MIMETYPE, "image/jpeg2000", "image/jpx", "image/jp2k"),
            decodeFull = { Jp2ImageDecoder.decode(it, 0, 0) },
            sizeOf = { Jp2ImageDecoder.getSize(it) },
        )

        fun forTiff(): Factory = Factory(
            mimeType = TIFF_MIMETYPE,
            supportedMimeTypes = setOf(TIFF_MIMETYPE, "image/tif", "image/x-tiff"),
            decodeFull = { TiffImageDecoder.decode(it, 0, 0) },
            sizeOf = { TiffImageDecoder.getSize(it) },
        )

        fun forSvg(): Factory = Factory(
            mimeType = SVG_MIMETYPE,
            supportedMimeTypes = setOf(SVG_MIMETYPE),
            decodeFull = { SvgImageDecoder.decode(it, SvgImageDecoder.REGION_MAX_DIM, SvgImageDecoder.REGION_MAX_DIM) },
            sizeOf = { SvgImageDecoder.renderSize(it, SvgImageDecoder.REGION_MAX_DIM, SvgImageDecoder.REGION_MAX_DIM) },
        )
    }
}
