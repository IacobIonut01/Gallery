package com.dot.gallery.core.decoder

import com.awxkee.jxlcoder.JxlCoder
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.asSketchImage
import com.github.panpf.sketch.decode.DecodeResult
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.internal.calculateSampleSize
import com.github.panpf.sketch.decode.internal.createInSampledTransformed
import com.github.panpf.sketch.decode.internal.isSmallerSizeMode
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.internal.RequestContext
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.util.Size
import okio.buffer

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
        val request = requestContext.request
        val sourceData = dataSource.openSource().buffer().readByteArray()
        val originalImageBitmap = JxlCoder.decode(sourceData)
        var imageInfo = ImageInfo(
            width = originalImageBitmap.width,
            height = originalImageBitmap.height,
            mimeType = "image/jxl",
        )

        val resize = requestContext.computeResize(imageInfo.size)
        val decodedImage = if (requestContext.size == Size.Origin) {
            originalImageBitmap
        } else {
            imageInfo = ImageInfo(
                width = resize.size.width,
                height = resize.size.height,
                mimeType = "image/jxl",
            )
            JxlCoder.decodeSampled(
                sourceData,
                resize.size.width,
                resize.size.height
            )
        }

        val size = requestContext.size!!
        val precision = request.precisionDecider.get(
            imageSize = Size(imageInfo.size.width, imageInfo.size.height),
            targetSize = size,
        )
        val inSampleSize = calculateSampleSize(
            imageSize = Size(imageInfo.size.width, imageInfo.size.height),
            targetSize = size,
            smallerSizeMode = precision.isSmallerSizeMode()
        )
        val transformeds: List<String>? =
            if (inSampleSize != 1) listOf(createInSampledTransformed(inSampleSize)) else null
        DecodeResult(
            image = decodedImage.asSketchImage(),
            imageInfo = imageInfo,
            dataFrom = dataSource.dataFrom,
            resize = requestContext.computeResize(if (size == Size.Origin) requestContext.size!! else size),
            transformeds = transformeds,
            extras = null
        )
    }

}