package com.dot.gallery.core.decoder

import android.graphics.Bitmap
import com.github.panpf.sketch.asSketchImage
import com.github.panpf.sketch.decode.DecodeResult
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.internal.appliedResize
import com.github.panpf.sketch.decode.internal.createScaledTransformed
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.util.computeScaleMultiplierWithOneSide
import okio.buffer
import kotlin.math.roundToInt

inline fun DataSource.withCustomDecoder(
    requestContext: RequestContext,
    mimeType: String,
    getSize: (ByteArray) -> android.util.Size?,
    decodeSampled: (ByteArray, Int, Int) -> Bitmap
): DecodeResult = openSource().use { src ->
    val sourceData = src.buffer().readByteArray()

    val imageInfo: ImageInfo
    var transformeds: List<String>? = null
    val originalSizeDecoded = getSize(sourceData) ?: android.util.Size(0, 0)
    val originalSize = Size(originalSizeDecoded.width, originalSizeDecoded.height)
    val targetSize = requestContext.size!!
    val scale = computeScaleMultiplierWithOneSide(
        sourceSize = originalSize,
        targetSize = targetSize,
    )
    if (scale != 1f) {
        transformeds = listOf(createScaledTransformed(scale))
    }

    val decodedImage = if (requestContext.size == Size.Origin) {
        imageInfo = ImageInfo(
            width = originalSize.width,
            height = originalSize.height,
            mimeType = mimeType,
        )
        decodeSampled(
            sourceData,
            originalSize.width,
            originalSize.height
        )
    } else {
        val dstSize = Size(
            width = (originalSize.width * scale).roundToInt(),
            height = (originalSize.height * scale).roundToInt()
        )
        imageInfo = ImageInfo(
            width = dstSize.width,
            height = dstSize.height,
            mimeType = mimeType,
        )
        decodeSampled(
            sourceData,
            dstSize.width,
            dstSize.height
        )
    }

    val resize = requestContext.computeResize(imageInfo.size)
    DecodeResult(
        image = decodedImage.asSketchImage(),
        imageInfo = imageInfo,
        dataFrom = dataFrom,
        resize = resize,
        transformeds = transformeds,
        extras = null
    ).appliedResize(requestContext)
}