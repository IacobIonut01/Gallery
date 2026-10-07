/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import android.graphics.Bitmap
import android.os.Build
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
import com.github.panpf.sketch.source.ContentDataSource
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.source.getFileOrNull
import com.github.panpf.sketch.util.Size
import okio.buffer
import com.dot.gallery.core.util.and
import com.dot.gallery.domain.util.isJp2
import com.dot.gallery.domain.util.isPsd


/**
 * Sketch decoders for formats with no native Android support, used by the media viewer (which is
 * Sketch-based). Detection is by magic bytes for PSD/JP2 (their MIME types are unreliable) and by
 * the reliable `image/tiff` MIME for TIFF (to avoid hijacking RAW formats that share TIFF headers).
 */

internal const val PSD_MIMETYPE = "image/vnd.adobe.photoshop"
internal const val JP2_MIMETYPE = "image/jp2"
internal const val TIFF_MIMETYPE = "image/tiff"
internal const val RAW_MIMETYPE = "image/x-raw"

fun ComponentRegistry.Builder.supportPsdDecoder(): ComponentRegistry.Builder = apply {
    add(SketchPsdDecoder.Factory())
}

fun ComponentRegistry.Builder.supportJp2Decoder(): ComponentRegistry.Builder = apply {
    add(SketchJp2Decoder.Factory())
}

fun ComponentRegistry.Builder.supportTiffDecoder(): ComponentRegistry.Builder = apply {
    add(SketchTiffDecoder.Factory())
}

fun ComponentRegistry.Builder.supportRawDecoder(): ComponentRegistry.Builder = apply {
    add(SketchRawDecoder.Factory())
}

/**
 * Camera-RAW MIME detection, mirroring [com.dot.gallery.domain.util.isRaw]
 * (`image/x-*` / `image/vnd.*`). Excludes PSD (own decoder) and BMP variants (natively decodable).
 */
internal fun isCameraRawMime(mime: String): Boolean {
    val m = mime.lowercase()
    if (m == PSD_MIMETYPE) return false
    if (m == "image/x-ms-bmp" || m == "image/x-bmp" || m == "image/x-windows-bmp") return false
    return m.startsWith("image/x-") || m.startsWith("image/vnd.")
}

internal fun DataSource.peekHeader(count: Int): Pair<ByteArray, Int> {
    openSource().buffer().use { source ->
        val peek = source.peek()
        val buf = ByteArray(count)
        val read = peek.read(buf)
        return buf to read
    }
}

/** Builds an [ImageData] from an already-decoded bitmap. */
internal fun DataSource.toImageData(
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

internal fun RequestContext.targetDims(): Pair<Int, Int> {
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