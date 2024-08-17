package com.dot.gallery.core.decoder

import android.graphics.ImageDecoder
import android.graphics.PostProcessor
import android.os.Build
import com.awxkee.jxlcoder.JxlCoder
import com.awxkee.jxlcoder.JxlResizeFilter
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.asSketchImage
import com.github.panpf.sketch.decode.DecodeResult
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.internal.calculateSampleSize
import com.github.panpf.sketch.decode.internal.createInSampledTransformed
import com.github.panpf.sketch.decode.internal.isSmallerSizeMode
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.animatedTransformation
import com.github.panpf.sketch.request.colorSpace
import com.github.panpf.sketch.request.internal.RequestContext
import com.github.panpf.sketch.source.AssetDataSource
import com.github.panpf.sketch.source.ByteArrayDataSource
import com.github.panpf.sketch.source.ContentDataSource
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.source.ResourceDataSource
import com.github.panpf.sketch.transform.AnimatedTransformation
import com.github.panpf.sketch.transform.flag
import com.github.panpf.sketch.util.Size
import okio.buffer
import java.nio.ByteBuffer

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
            return if (fetchResult.mimeType in AVAILABLE_MIME_TYPES) {
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
            private val AVAILABLE_MIME_TYPES = listOf(
                "image/jxl"
            )
        }
    }

    override suspend fun decode(): Result<DecodeResult> = kotlin.runCatching {
        val request = requestContext.request
        val sourceData = dataSource.openSource().buffer().readByteArray()
        val source = when (dataSource) {
            is AssetDataSource -> {
                ImageDecoder.createSource(request.context.assets, dataSource.fileName)
            }

            is ResourceDataSource -> {
                ImageDecoder.createSource(dataSource.resources, dataSource.resId)
            }

            is ContentDataSource -> {
                ImageDecoder.createSource(request.context.contentResolver, dataSource.contentUri)
            }

            is ByteArrayDataSource -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ImageDecoder.createSource(dataSource.data)
                } else {
                    ImageDecoder.createSource(ByteBuffer.wrap(dataSource.data))
                }
            }

            else -> {
                dataSource.getFileOrNull()
                    ?.let { ImageDecoder.createSource(it.toFile()) }
                    ?: throw Exception("Unsupported DataSource: ${dataSource::class}")
            }
        }


        var imageInfo: ImageInfo? = null
        var inSampleSize = 1
        var imageDecoder: ImageDecoder? = null
        try {
            ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                imageDecoder = decoder
                imageInfo = ImageInfo(
                    width = info.size.width,
                    height = info.size.height,
                    mimeType = info.mimeType,
                )
                val size = requestContext.size!!
                val precision = request.precisionDecider.get(
                    imageSize = Size(info.size.width, info.size.height),
                    targetSize = size,
                )
                inSampleSize = calculateSampleSize(
                    imageSize = Size(info.size.width, info.size.height),
                    targetSize = size,
                    smallerSizeMode = precision.isSmallerSizeMode()
                )
                decoder.setTargetSampleSize(inSampleSize)

                request.colorSpace?.let {
                    decoder.setTargetColorSpace(it)
                }

                // Set the animated transformation to be applied on each frame.
                decoder.postProcessor = request.animatedTransformation?.asPostProcessor()
            }
        } finally {
            imageDecoder?.close()
        }
        val transformeds: List<String>? =
            if (inSampleSize != 1) listOf(createInSampledTransformed(inSampleSize)) else null

        if (requestContext.size == Size.Origin) {
            val originalImage =
                JxlCoder.decode(
                    sourceData
                )
            return@runCatching DecodeResult(
                image = originalImage.asSketchImage(),
                imageInfo = imageInfo!!,
                dataFrom = dataSource.dataFrom,
                resize = requestContext.computeResize(requestContext.size!!),
                transformeds = transformeds,
                extras = null
            )
        }

        val resize = requestContext.computeResize(imageInfo!!.size)
        val originalImage =
            JxlCoder.decodeSampled(
                sourceData,
                resize.size.width,
                resize.size.height,
                scaleMode = com.awxkee.jxlcoder.ScaleMode.FIT,
                jxlResizeFilter = JxlResizeFilter.BILINEAR
            )

        DecodeResult(
            image = originalImage.asSketchImage(),
            imageInfo = imageInfo!!,
            dataFrom = dataSource.dataFrom,
            resize = resize,
            transformeds = transformeds,
            extras = null
        )

    }


    private fun AnimatedTransformation.asPostProcessor() =
        PostProcessor { canvas -> transform(canvas).flag }
}