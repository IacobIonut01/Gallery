package com.dot.gallery.core.decoder

import com.dot.gallery.core.decoder.SketchHeifDecoder.Factory.Companion.HEIF_MIMETYPES
import com.dot.gallery.core.decoder.SketchJxlDecoder.Factory.Companion.JXL_MIMETYPE
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
import com.github.panpf.sketch.source.ContentDataSource
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.util.MimeTypeMap
import com.github.panpf.sketch.util.Size


fun ComponentRegistry.Builder.supportThumbnailDecoder(): ComponentRegistry.Builder = apply {
    addDecoder(ThumbnailDecoder.Factory())
}

class ThumbnailDecoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
) : Decoder {

    class Factory : Decoder.Factory {

        override val key: String
            get() = "ThumbnailDecoder"

        override fun create(requestContext: RequestContext, fetchResult: FetchResult): Decoder? {
            val mimeType = fetchResult.mimeType
            return if (
                mimeType != null &&
                mimeType.isVideoOrImage &&
                !isSvg(fetchResult) &&
                !isSpecialFormat(fetchResult) &&
                fetchResult.dataSource is ContentDataSource
            )
                ThumbnailDecoder(requestContext, fetchResult.dataSource)
            else null
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Factory
        }

        override fun hashCode(): Int {
            return this@Factory::class.hashCode()
        }

        override fun toString(): String = key

        private val String.isVideoOrImage get() = startsWith("video/") || startsWith("image/")

        private fun isSvg(result: FetchResult) =
            result.mimeType?.contains(MIME_TYPE_SVG) == true

        private fun isSpecialFormat(result: FetchResult) =
            HEIF_MIMETYPES.any { result.mimeType?.contains(it) == true } || result.mimeType?.contains(JXL_MIMETYPE) == true

        companion object {
            private const val MIME_TYPE_SVG = "image/svg"
        }
    }

    override suspend fun decode(): Result<DecodeResult> = runCatching {
        val request = requestContext.request
        val dataSource = (dataSource as ContentDataSource)

        val size = requestContext.size
        val bitmap = request.context.contentResolver.loadThumbnail(
            dataSource.contentUri,
            android.util.Size(size!!.width, size.height),
            null
        )
        val mimeType = MimeTypeMap.getMimeTypeFromUrl(dataSource.contentUri.toString()).toString()
        val imageSize = Size(bitmap.width, bitmap.height)
        val precision = request.precisionDecider.get(
            imageSize = imageSize,
            targetSize = size,
        )
        val inSampleSize = calculateSampleSize(
            imageSize = imageSize,
            targetSize = requestContext.size!!,
            smallerSizeMode = precision.isSmallerSizeMode()
        )

        DecodeResult(
            image = bitmap.asSketchImage(),
            imageInfo = ImageInfo(
                width = bitmap.width,
                height = bitmap.height,
                mimeType = mimeType
            ),
            dataFrom = dataSource.dataFrom,
            resize = requestContext.computeResize(requestContext.size!!),
            transformeds = if (inSampleSize != 1) listOf(createInSampledTransformed(inSampleSize)) else null,
            extras = null
        )
    }

}
