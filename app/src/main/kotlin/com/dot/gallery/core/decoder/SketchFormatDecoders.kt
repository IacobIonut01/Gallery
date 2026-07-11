/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import android.graphics.Bitmap
import android.os.Build
import com.dot.gallery.core.decoder.format.CameraRawImageDecoder
import com.dot.gallery.core.decoder.format.CameraRawMime
import com.dot.gallery.core.decoder.format.ImageFormatSniffer
import com.dot.gallery.core.decoder.format.Jp2ImageDecoder
import com.dot.gallery.core.decoder.format.PsdImageDecoder
import com.dot.gallery.core.decoder.format.TiffImageDecoder
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.asImage
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.ImageData
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.request.get
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.util.Size
import okio.buffer

/**
 * Sketch decoders for formats with no native Android support, used by the media viewer (which is
 * Sketch-based). Detection is by magic bytes for PSD/JP2 (their MIME types are unreliable) and by
 * the reliable `image/tiff` MIME for TIFF (to avoid hijacking RAW formats that share TIFF headers).
 */

private const val PSD_MIMETYPE = "image/vnd.adobe.photoshop"
private const val JP2_MIMETYPE = "image/jp2"
private const val TIFF_MIMETYPE = "image/tiff"

fun ComponentRegistry.Builder.supportPsdDecoder(): ComponentRegistry.Builder = apply {
    add(SketchPsdDecoder.Factory())
}

fun ComponentRegistry.Builder.supportJp2Decoder(): ComponentRegistry.Builder = apply {
    add(SketchJp2Decoder.Factory())
}

fun ComponentRegistry.Builder.supportTiffDecoder(): ComponentRegistry.Builder = apply {
    add(SketchTiffDecoder.Factory())
}

fun ComponentRegistry.Builder.supportCameraRawDecoder(): ComponentRegistry.Builder = apply {
    add(SketchCameraRawDecoder.Factory())
}

private fun DataSource.peekHeader(count: Int): Pair<ByteArray, Int> {
    openSource().buffer().use { source ->
        val peek = source.peek()
        val buf = ByteArray(count)
        val read = peek.read(buf)
        return buf to read
    }
}

/** Builds an [ImageData] from an already-decoded bitmap. */
private fun DataSource.toImageData(
    bitmap: Bitmap,
    mimeType: String,
    requestContext: RequestContext,
): ImageData {
    val imageInfo = ImageInfo(bitmap.width, bitmap.height, mimeType)
    val resize = requestContext.computeResize(imageInfo.size)
    return ImageData(
        image = bitmap.asImage(),
        imageInfo = imageInfo,
        dataFrom = dataFrom,
        resize = resize,
        transformeds = null,
        extras = null
    )
}

private fun RequestContext.targetDims(): Pair<Int, Int> {
    val target = size
    return if (target == Size.Origin) 0 to 0 else target.width to target.height
}

class SketchPsdDecoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
) : Decoder {

    class Factory : Decoder.Factory {
        override val key: String get() = "PsdDecoder"
        override val sortWeight: Int = 0

        override fun create(requestContext: RequestContext, fetchResult: FetchResult): Decoder? {
            val (head, read) = fetchResult.dataSource.peekHeader(8)
            return if (read >= 4 && ImageFormatSniffer.isPsd(head, read)) {
                SketchPsdDecoder(requestContext, fetchResult.dataSource)
            } else null
        }

        override fun equals(other: Any?): Boolean = this === other || other is Factory
        override fun hashCode(): Int = this@Factory::class.hashCode()
        override fun toString(): String = key
    }

    override suspend fun decode(): ImageData {
        val bytes = dataSource.openSource().buffer().use { it.readByteArray() }
        val (w, h) = requestContext.targetDims()
        val bitmap = PsdImageDecoder.decode(bytes, w, h)
            ?: throw IllegalStateException("Unable to decode PSD image")
        return dataSource.toImageData(bitmap, PSD_MIMETYPE, requestContext)
    }

    override suspend fun getImageInfo(): ImageInfo {
        val bytes = dataSource.openSource().buffer().use { it.readByteArray() }
        val size = PsdImageDecoder.getSize(bytes)
        return ImageInfo(size?.width ?: 0, size?.height ?: 0, PSD_MIMETYPE)
    }
}

class SketchJp2Decoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
) : Decoder {

    class Factory : Decoder.Factory {
        override val key: String get() = "Jp2Decoder"
        override val sortWeight: Int = 0

        override fun create(requestContext: RequestContext, fetchResult: FetchResult): Decoder? {
            val (head, read) = fetchResult.dataSource.peekHeader(12)
            return if (read >= 4 && ImageFormatSniffer.isJp2(head, read)) {
                SketchJp2Decoder(requestContext, fetchResult.dataSource)
            } else null
        }

        override fun equals(other: Any?): Boolean = this === other || other is Factory
        override fun hashCode(): Int = this@Factory::class.hashCode()
        override fun toString(): String = key
    }

    override suspend fun decode(): ImageData {
        val bytes = dataSource.openSource().buffer().use { it.readByteArray() }
        val (w, h) = requestContext.targetDims()
        val bitmap = Jp2ImageDecoder.decode(bytes, w, h)
            ?: throw IllegalStateException("Unable to decode JPEG 2000 image")
        return dataSource.toImageData(bitmap, JP2_MIMETYPE, requestContext)
    }

    override suspend fun getImageInfo(): ImageInfo {
        val bytes = dataSource.openSource().buffer().use { it.readByteArray() }
        val size = Jp2ImageDecoder.getSize(bytes)
        return ImageInfo(size?.width ?: 0, size?.height ?: 0, JP2_MIMETYPE)
    }
}

class SketchTiffDecoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
) : Decoder {

    class Factory : Decoder.Factory {
        override val key: String get() = "TiffDecoder"
        override val sortWeight: Int = 0

        override fun create(requestContext: RequestContext, fetchResult: FetchResult): Decoder? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            val realMime = requestContext.request.extras?.get("realMimeType") as String?
            val mime = realMime ?: fetchResult.mimeType
            return if (mime != null && (mime.contains("image/tiff") || mime.contains("image/tif"))) {
                SketchTiffDecoder(requestContext, fetchResult.dataSource)
            } else null
        }

        override fun equals(other: Any?): Boolean = this === other || other is Factory
        override fun hashCode(): Int = this@Factory::class.hashCode()
        override fun toString(): String = key
    }

    override suspend fun decode(): ImageData {
        val bytes = dataSource.openSource().buffer().use { it.readByteArray() }
        val (w, h) = requestContext.targetDims()
        val bitmap = TiffImageDecoder.decode(bytes, w, h)
            ?: throw IllegalStateException("Unable to decode TIFF image")
        return dataSource.toImageData(bitmap, TIFF_MIMETYPE, requestContext)
    }

    override suspend fun getImageInfo(): ImageInfo {
        val bytes = dataSource.openSource().buffer().use { it.readByteArray() }
        val size = TiffImageDecoder.getSize(bytes)
        return ImageInfo(size?.width ?: 0, size?.height ?: 0, TIFF_MIMETYPE)
    }
}

class SketchCameraRawDecoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
    private val mimeType: String,
) : Decoder {

    class Factory : Decoder.Factory {
        override val key: String get() = "CameraRawDecoder"
        override val sortWeight: Int = 0

        override fun create(requestContext: RequestContext, fetchResult: FetchResult): Decoder? {
            val realMime = requestContext.request.extras?.get("realMimeType") as String?
            val mime = realMime ?: fetchResult.mimeType
            if (CameraRawMime.isCameraRaw(mime)) {
                return SketchCameraRawDecoder(requestContext, fetchResult.dataSource, mime!!)
            }
            return null
        }

        override fun equals(other: Any?): Boolean = this === other || other is Factory
        override fun hashCode(): Int = this@Factory::class.hashCode()
        override fun toString(): String = key
    }

    override suspend fun decode(): ImageData {
        val bytes = dataSource.openSource().buffer().use { it.readByteArray() }
        val (w, h) = requestContext.targetDims()
        val bitmap = CameraRawImageDecoder.decode(bytes, w, h, mimeType)
            ?: throw IllegalStateException("Unable to decode camera RAW image")
        return dataSource.toImageData(bitmap, mimeType, requestContext)
    }

    override suspend fun getImageInfo(): ImageInfo {
        val bytes = dataSource.openSource().buffer().use { it.readByteArray() }
        val size = CameraRawImageDecoder.getSize(bytes, mimeType)
        return ImageInfo(size?.width ?: 0, size?.height ?: 0, mimeType)
    }
}
