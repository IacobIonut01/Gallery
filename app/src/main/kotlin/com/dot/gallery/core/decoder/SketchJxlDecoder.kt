package com.dot.gallery.core.decoder

import com.awxkee.jxlcoder.JxlCoder
import com.dot.gallery.core.decoder.SketchJxlDecoder.Factory.Companion.JXL_MIMETYPE
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.decode.DecodeResult
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.source.DataSource

fun ComponentRegistry.Builder.supportJxlDecoder(): ComponentRegistry.Builder = apply {
    addDecoder(SketchJxlDecoder.Factory())
}

class SketchJxlDecoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
) : Decoder {

    class Factory : Decoder.Factory {

        override val key: String
            get() = "JxlDecoder"

        override fun create(requestContext: RequestContext, fetchResult: FetchResult): Decoder? {
            return if (fetchResult.mimeType?.contains(JXL_MIMETYPE) == true) {
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

    override suspend fun decode(): Result<DecodeResult> = kotlin.runCatching {
        return@runCatching dataSource.withCustomDecoder(
            requestContext = requestContext,
            mimeType = JXL_MIMETYPE,
            getSize = JxlCoder::getSize,
            decodeSampled = JxlCoder::decodeSampled
        )
    }

}