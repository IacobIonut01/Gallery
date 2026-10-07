package com.dot.gallery.core.decoder

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.BitmapParams
import androidx.exifinterface.media.ExifInterface
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.Image
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.asImage
import com.github.panpf.sketch.decode.DecodeConfig
import com.github.panpf.sketch.decode.DecodeException
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.internal.DecodeHelper
import com.github.panpf.sketch.decode.internal.ExifOrientationHelper
import com.github.panpf.sketch.decode.internal.HelperDecoder
import com.github.panpf.sketch.decode.internal.checkImageInfo
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.request.videoFrameOption
import com.github.panpf.sketch.source.ContentDataSource
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.source.getFileOrNull
import com.github.panpf.sketch.util.Rect
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.util.div
import com.dot.gallery.core.util.and
import com.dot.gallery.core.util.or
import com.dot.gallery.domain.uri.path

/**
 * Use MediaMetadataRetriever to decode video frames
 *
 * The following decoding related properties are supported:
 *
 * * sizeResolver: Only sampleSize
 * * sizeMultiplier
 * * precisionDecider: Only LESS_PIXELS and SMALLER_SIZE is supported
 * * colorSpace: Only on Android 30 or later
 * * videoFrameMicros
 * * videoFramePercent
 * * videoFrameOption
 *
 * The following decoding related properties are not supported:
 *
 * * scaleDecider
 * * colorType
 *
 * @see com.github.panpf.sketch.video.test.decode.internal.VideoFrameDecodeHelperTest
 */
class VideoFrameDecodeHelper2(
    val sketch: Sketch,
    val request: ImageRequest,
    val dataSource: DataSource,
    private val mimeType: String,
) : DecodeHelper {

    private var _cachedImageInfo: ImageInfo? = null

    override suspend fun getImageInfo(): ImageInfo {
        return _cachedImageInfo ?: readImageInfo().also { _cachedImageInfo = it }
    }

    override suspend fun isSupportRegion(): Boolean = false

    private val mediaMetadataRetriever by lazy {
        MediaMetadataRetriever().apply {
            if (dataSource is ContentDataSource) {
                setDataSource(request.context, dataSource.contentUri)
            } else {
                dataSource.getFileOrNull(sketch)?.let { setDataSource(it.toFile().path) }
                    ?: throw Exception("Unsupported DataSource: ${dataSource::class}")
            }
        }
    }
    private val exifOrientation: Int by lazy { readExifOrientation() }
    private val exifOrientationHelper by lazy { ExifOrientationHelper(exifOrientation) }

    @SuppressLint("NewApi")
    override suspend fun decode(sampleSize: Int): Image {
        val frameMicros = 0L
        val option = request.videoFrameOption ?: MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        val imageSize = getImageInfo().size
        val dstSize = imageSize / sampleSize.toFloat()

        val config = DecodeConfig(request, getImageInfo().mimeType, isOpaque = false)
        val bitmapParams = BitmapParams().apply {
            config.colorType?.also { preferredConfig = it }
        }
        val bitmap = mediaMetadataRetriever.getScaledFrameAtTime(
            /* timeUs = */ frameMicros,
            /* option = */ option,
            /* dstWidth = */ dstSize.width,
            /* dstHeight = */ dstSize.height,
            /* params = */ bitmapParams
        ) ?: throw DecodeException(
            "Failed to getScaledFrameAtTime. " +
                    "frameMicros=$frameMicros, " +
                    "option=${optionToName(option)}, " +
                    "dstSize=$dstSize, " +
                    "imageSize=$imageSize, " +
                    "preferredConfig=${config.colorType}"
        )

        val correctedBitmap = exifOrientationHelper.applyToBitmap(bitmap) ?: bitmap
        return correctedBitmap.asImage()
    }

    override suspend fun decodeRegion(region: Rect, sampleSize: Int): Image {
        throw UnsupportedOperationException("Unsupported region decode")
    }

    private fun readImageInfo(): ImageInfo {
        val srcWidth = mediaMetadataRetriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val srcHeight = mediaMetadataRetriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val imageSize = Size(width = srcWidth, height = srcHeight)
        val correctedImageSize = exifOrientationHelper.applyToSize(imageSize)
        return ImageInfo(size = correctedImageSize, mimeType = mimeType)
            .apply { checkImageInfo(this) }
    }

    private fun readExifOrientation(): Int {
        val videoRotation = mediaMetadataRetriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull() ?: 0
        val exifOrientation = when (videoRotation) {
            90 -> ExifInterface.ORIENTATION_ROTATE_90
            180 -> ExifInterface.ORIENTATION_ROTATE_180
            270 -> ExifInterface.ORIENTATION_NORMAL
            else -> ExifInterface.ORIENTATION_NORMAL
        }
        return exifOrientation
    }

    private fun optionToName(option: Int): String {
        return when (option) {
            MediaMetadataRetriever.OPTION_CLOSEST -> "CLOSEST"
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC -> "CLOSEST_SYNC"
            MediaMetadataRetriever.OPTION_NEXT_SYNC -> "NEXT_SYNC"
            MediaMetadataRetriever.OPTION_PREVIOUS_SYNC -> "PREVIOUS_SYNC"
            else -> "Unknown($option)"
        }
    }

    override fun toString(): String {
        return "VideoFrameDecodeHelper2(request=$request, dataSource=$dataSource, mimeType=$mimeType)"
    }

    override fun close() {
        mediaMetadataRetriever.close()
    }
}