package com.dot.gallery.core.decoder

import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.asSketchImage
import com.github.panpf.sketch.decode.DecodeResult
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.internal.calculateSampleSize
import com.github.panpf.sketch.decode.internal.createInSampledTransformed
import com.github.panpf.sketch.decode.internal.isSmallerSizeMode
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.util.Size
import com.radzivon.bartoshyk.avif.coder.HeifCoder
import com.radzivon.bartoshyk.avif.coder.PreferredColorConfig
import okio.buffer

fun ComponentRegistry.Builder.supportHeifDecoder(): ComponentRegistry.Builder = apply {
    addDecoder(SketchHeifDecoder.Factory())
}

class SketchHeifDecoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
    private val mimeType: String
) : Decoder {

    class Factory : Decoder.Factory {

        override val key: String
            get() = "HeifDecoder"

        override fun create(requestContext: RequestContext, fetchResult: FetchResult): Decoder? {
            return if (HEIF_MIMETYPES.any { fetchResult.mimeType?.contains(it) == true }) {
                SketchHeifDecoder(requestContext, fetchResult.dataSource, fetchResult.mimeType!!)
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
            val HEIF_MIMETYPES = listOf(
                "image/heif",
                "image/heic",
                "image/heif-sequence",
                "image/heic-sequence",
                "image/avif",
                "image/avis"
            )
        }
    }

    override suspend fun decode(): Result<DecodeResult> = runCatching {
        val coder = HeifCoder(requestContext.request.context)
        dataSource.openSource().use { src ->
            val sourceData = src.buffer().readByteArray()

            val request = requestContext.request

            val imageInfo: ImageInfo

            val size: Size

            val decodedImage = if (requestContext.size == Size.Origin) {
                val originalImageBitmap = coder.decode(sourceData)
                size = Size(originalImageBitmap.width, originalImageBitmap.height)
                imageInfo = ImageInfo(
                    width = size.width,
                    height = size.height,
                    mimeType = mimeType,
                )
                originalImageBitmap
            } else {
                val resize = requestContext.computeResize(requestContext.size!!)
                size = resize.size
                imageInfo = ImageInfo(
                    width = size.width,
                    height = size.height,
                    mimeType = mimeType,
                )
                coder.decodeSampled(
                    sourceData,
                    size.width,
                    size.height,
                    preferredColorConfig = PreferredColorConfig.RGBA_8888
                )
            }

            val precision = request.precisionDecider.get(
                imageSize = Size(imageInfo.size.width, imageInfo.size.height),
                targetSize = size,
            )
            val inSampleSize = calculateSampleSize(
                imageSize = Size(imageInfo.size.width, imageInfo.size.height),
                targetSize = size,
                smallerSizeMode = precision.isSmallerSizeMode()
            )

            DecodeResult(
                image = decodedImage.asSketchImage(),
                imageInfo = imageInfo,
                dataFrom = dataSource.dataFrom,
                resize = requestContext.computeResize(if (size == Size.Origin) requestContext.size!! else size),
                transformeds = if (inSampleSize != 1) listOf(createInSampledTransformed(inSampleSize)) else null,
                extras = null
            )
        }
    }

}