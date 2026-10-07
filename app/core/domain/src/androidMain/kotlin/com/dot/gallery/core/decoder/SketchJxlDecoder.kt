package com.dot.gallery.core.decoder

import com.awxkee.jxlcoder.JxlCoder
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.request.ImageData
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.request.get
import com.github.panpf.sketch.source.DataSource
import com.dot.gallery.core.decoder.SketchJxlDecoder.Factory.Companion.JXL_MIMETYPE



fun ComponentRegistry.Builder.supportJxlDecoder(): ComponentRegistry.Builder = apply {
    add(SketchJxlDecoder.Factory())
}

class SketchJxlDecoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
) : Decoder {

    class Factory : Decoder.Factory {

        override val key: String
            get() = "JxlDecoder"

        override val sortWeight: Int = 0

        override fun create(requestContext: RequestContext, fetchResult: FetchResult): Decoder? {
            val mimeType = requestContext.request.extras?.get("realMimeType") as String? ?: return null
            return if (mimeType.contains(JXL_MIMETYPE)) {
                SketchJxlDecoder(requestContext, fetchResult.dataSource)
            } else {
                null
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Factory
        }

        override fun hashCode(): Int {
            return this@Factory::class.hashCode()
        }

        override fun toString(): String = key

        companion object {
            const val JXL_MIMETYPE = "image/jxl"
        }

    }

    override suspend fun decode(): ImageData {
        return dataSource.withCustomDecoder(
            requestContext = requestContext,
            mimeType = JXL_MIMETYPE,
            getSize = JxlCoder::getSize,
            decodeSampled = JxlCoder::decodeSampled
        )
    }

    override suspend fun getImageInfo(): ImageInfo {
        return dataSource.getImageInfo(
            requestContext = requestContext,
            mimeType = JXL_MIMETYPE,
            getSize = JxlCoder::getSize
        )
    }

}