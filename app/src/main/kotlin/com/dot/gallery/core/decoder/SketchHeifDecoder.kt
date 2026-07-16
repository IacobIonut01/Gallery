package com.dot.gallery.core.decoder

import android.os.Build
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.request.ImageData
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.request.get
import com.github.panpf.sketch.source.DataSource
import com.dot.gallery.core.decoder.format.HeifDecodeEngine
import okio.buffer

fun ComponentRegistry.Builder.supportHeifDecoder(): ComponentRegistry.Builder = apply {
    add(SketchHeifDecoder.Factory())
}

@Suppress("SpellCheckingInspection")
class SketchHeifDecoder(
    private val requestContext: RequestContext,
    private val dataSource: DataSource,
    private val mimeType: String
) : Decoder {

    class Factory : Decoder.Factory {

        override val key: String
            get() = "HeifDecoder"

        override val sortWeight: Int = 0

        override fun create(requestContext: RequestContext, fetchResult: FetchResult): Decoder? {
            val mimeType = requestContext.request.extras?.get("realMimeType") as String? ?: return null
            return if (HEIF_MIMETYPES.any { mimeType.contains(it) }) {
                SketchHeifDecoder(requestContext, fetchResult.dataSource, fetchResult.mimeType ?: mimeType)
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

    override suspend fun decode(): ImageData {
        val sourceData = dataSource.openSource().use { src ->
            src.buffer().readByteArray()
        }

        // Animated HEIC/AVIF sequence: requires API 31+ for ImageDecoder sequence support. Returns
        // null (falls through to a static decode) when the platform can't animate this container.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isAnimatedHeif(sourceData)) {
            val animated = decodeAnimatedHeif(
                bytes = sourceData,
                requestContext = requestContext,
                dataFrom = dataSource.dataFrom,
                mimeType = mimeType,
                getSize = HeifDecodeEngine::getSize
            )
            if (animated != null) return animated
        }

        // Hardware-first, software fallback (unified engine). This is the media viewer's base
        // painter (and the gain-map probe that drives per-page COLOR_MODE_HDR), so decode with HDR
        // ENABLED: keep any Ultra HDR gain map / 10-bit HLG/PQ color space so the fit-view image
        // renders true HDR on a capable display. Zoomed subsampling tiles remain SDR (region
        // decoders can't reproduce the gain map), so a slight brightness shift can appear when
        // zooming into HDR highlights — an accepted tradeoff, since the HDR pop matters most at fit
        // view. The grid (Glide) path stays allowHdr=false. On SDR displays / SDR images the gain
        // map is simply not applied, so this is a no-op there.
        val target = requestContext.size
        val reqW = if (target == com.github.panpf.sketch.util.Size.Origin) 0 else target.width
        val reqH = if (target == com.github.panpf.sketch.util.Size.Origin) 0 else target.height
        HeifDecodeEngine.decode(sourceData, reqW, reqH, allowHdr = true)?.let {
            return imageDataFromBitmap(it, requestContext, dataSource.dataFrom, mimeType)
        }

        // Last resort: software-only path via the engine (also drives the request's scaled resize).
        return decodeStaticFromBytes(
            sourceData = sourceData,
            requestContext = requestContext,
            dataFrom = dataSource.dataFrom,
            mimeType = mimeType,
            getSize = HeifDecodeEngine::getSize,
            decodeSampled = { bytes, w, h ->
                HeifDecodeEngine.decodeSoftware(bytes, w, h)
                    ?: throw IllegalStateException("Unable to decode HEIF image")
            }
        )
    }

    override suspend fun getImageInfo(): ImageInfo {
        return dataSource.getImageInfo(
            requestContext = requestContext,
            mimeType = mimeType,
            getSize = HeifDecodeEngine::getSize
        )
    }

}