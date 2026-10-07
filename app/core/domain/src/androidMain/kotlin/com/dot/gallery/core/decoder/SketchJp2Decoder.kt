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