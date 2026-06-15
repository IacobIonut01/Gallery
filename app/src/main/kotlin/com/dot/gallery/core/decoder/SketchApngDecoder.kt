package com.dot.gallery.core.decoder

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.asImage
import com.github.panpf.sketch.decode.DecodeConfig
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.internal.readImageInfoWithIgnoreExifOrientation
import com.github.panpf.sketch.drawable.ScaledAnimatableDrawable
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.ANIMATION_REPEAT_INFINITE
import com.github.panpf.sketch.request.ImageData
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.request.animationEndCallback
import com.github.panpf.sketch.request.animationStartCallback
import com.github.panpf.sketch.request.disallowAnimatedImage
import com.github.panpf.sketch.request.get
import com.github.panpf.sketch.request.repeatCount
import com.github.panpf.sketch.source.AssetDataSource
import com.github.panpf.sketch.source.ByteArrayDataSource
import com.github.panpf.sketch.source.ContentDataSource
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.source.ResourceDataSource
import com.github.panpf.sketch.source.getFileOrNull
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.util.animatable2CompatCallbackOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * Extension function to add APNG animated image support to Sketch.
 * Uses Android's ImageDecoder (API 28+) which natively decodes APNG
 * into AnimatedImageDrawable.
 */
@RequiresApi(Build.VERSION_CODES.P)
fun ComponentRegistry.Builder.supportApng(): ComponentRegistry.Builder = apply {
    add(SketchApngDecoder.Factory())
}

/**
 * Decoder for Animated PNG (APNG) images using Android's ImageDecoder.
 *
 * Uses the same direct-decode approach as [decodeAnimatedAvif] to ensure
 * animation works correctly with the compose rendering pipeline.
 */
@RequiresApi(Build.VERSION_CODES.P)
class SketchApngDecoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
) : Decoder {

    @WorkerThread
    override suspend fun decode(): ImageData {
        val context = requestContext.request.context
        val source = when (dataSource) {
            is AssetDataSource -> {
                ImageDecoder.createSource(context.assets, dataSource.fileName)
            }
            is ResourceDataSource -> {
                ImageDecoder.createSource(dataSource.resources, dataSource.resId)
            }
            is ContentDataSource -> {
                ImageDecoder.createSource(context.contentResolver, dataSource.contentUri)
            }
            is ByteArrayDataSource -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ImageDecoder.createSource(dataSource.data)
                } else {
                    ImageDecoder.createSource(ByteBuffer.wrap(dataSource.data))
                }
            }
            else -> {
                val file = dataSource.getFileOrNull(requestContext.sketch)
                    ?: throw Exception("Unsupported DataSource: ${dataSource::class}")
                ImageDecoder.createSource(file.toFile())
            }
        }

        var imageInfo: ImageInfo? = null
        var imageDecoder: ImageDecoder? = null
        val request = requestContext.request
        val drawable = try {
            ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                imageDecoder = decoder
                imageInfo = ImageInfo(
                    width = info.size.width,
                    height = info.size.height,
                    mimeType = info.mimeType,
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val decodeConfig = DecodeConfig(request, info.mimeType, isOpaque = true)
                decodeConfig.colorSpace?.let { decoder.setTargetColorSpace(it) }
            }
        } finally {
            imageDecoder?.close()
        }
        requireNotNull(imageInfo)

        if (drawable !is AnimatedImageDrawable) {
            // Not animated, fall back to static rendering
            return ImageData(
                image = drawable.asImage(),
                imageInfo = imageInfo,
                dataFrom = dataSource.dataFrom,
                resize = requestContext.computeResize(Size(imageInfo!!.width, imageInfo!!.height)),
                transformeds = null,
                extras = null,
            )
        }

        drawable.repeatCount = request.repeatCount
            ?.takeIf { it != ANIMATION_REPEAT_INFINITE }
            ?: AnimatedImageDrawable.REPEAT_INFINITE

        @Suppress("OPT_IN_USAGE")
        val scaledDrawable = ScaledAnimatableDrawable(drawable).apply {
            val onStart = request.animationStartCallback
            val onEnd = request.animationEndCallback
            if (onStart != null || onEnd != null) {
                GlobalScope.launch(Dispatchers.Main) {
                    registerAnimationCallback(animatable2CompatCallbackOf(onStart, onEnd))
                }
            }
        }

        val imageSize = Size(imageInfo!!.width, imageInfo!!.height)
        val resize = requestContext.computeResize(imageSize)

        return ImageData(
            image = scaledDrawable.asImage(),
            imageInfo = imageInfo,
            dataFrom = dataSource.dataFrom,
            resize = resize,
            transformeds = null,
            extras = null,
        )
    }

    override suspend fun getImageInfo(): ImageInfo {
        return dataSource.readImageInfoWithIgnoreExifOrientation()
    }

    companion object {
        const val SORT_WEIGHT = 14

        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )

        private val ACTL_CHUNK = byteArrayOf(0x61, 0x63, 0x54, 0x4C)

        fun ByteArray.isPng(): Boolean {
            if (size < PNG_SIGNATURE.size) return false
            return PNG_SIGNATURE.indices.all { this[it] == PNG_SIGNATURE[it] }
        }

        fun ByteArray.isApng(): Boolean {
            if (!isPng()) return false
            var offset = 8
            while (offset + 8 <= size) {
                val length = ((this[offset].toInt() and 0xFF) shl 24) or
                        ((this[offset + 1].toInt() and 0xFF) shl 16) or
                        ((this[offset + 2].toInt() and 0xFF) shl 8) or
                        (this[offset + 3].toInt() and 0xFF)

                val typeOffset = offset + 4
                if (typeOffset + 4 > size) break

                if (this[typeOffset] == ACTL_CHUNK[0] &&
                    this[typeOffset + 1] == ACTL_CHUNK[1] &&
                    this[typeOffset + 2] == ACTL_CHUNK[2] &&
                    this[typeOffset + 3] == ACTL_CHUNK[3]
                ) {
                    return true
                }

                if (this[typeOffset] == 0x49.toByte() &&
                    this[typeOffset + 1] == 0x44.toByte() &&
                    this[typeOffset + 2] == 0x41.toByte() &&
                    this[typeOffset + 3] == 0x54.toByte()
                ) {
                    return false
                }

                offset += 4 + 4 + length + 4
            }
            return false
        }
    }

    class Factory : Decoder.Factory {

        override val key: String = "SketchApngDecoder"
        override val sortWeight: Int = SORT_WEIGHT

        override fun create(
            requestContext: RequestContext,
            fetchResult: FetchResult
        ): SketchApngDecoder? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            if (requestContext.request.disallowAnimatedImage == true) return null
            if (!isApplicable(requestContext, fetchResult)) return null
            return SketchApngDecoder(requestContext, fetchResult.dataSource)
        }

        private fun isApplicable(requestContext: RequestContext, fetchResult: FetchResult): Boolean {
            val realMimeType = requestContext.request.extras?.get("realMimeType") as? String
            if (realMimeType == "image/apng") return true
            if (fetchResult.mimeType == "image/apng") return true
            return fetchResult.headerBytes.isApng()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other != null && this::class == other::class
        }

        override fun hashCode(): Int {
            return this::class.hashCode()
        }

        override fun toString(): String = "SketchApngDecoder"
    }
}
