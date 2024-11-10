package com.dot.gallery.core.decoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.exifinterface.media.ExifInterface
import com.dot.gallery.BuildConfig
import com.dot.gallery.feature_node.data.data_source.KeychainHolder
import com.dot.gallery.feature_node.domain.model.EncryptedMedia
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.Image
import com.github.panpf.sketch.asSketchImage
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.ImageInvalidException
import com.github.panpf.sketch.decode.internal.DecodeHelper
import com.github.panpf.sketch.decode.internal.ExifOrientationHelper
import com.github.panpf.sketch.decode.internal.HelperDecoder
import com.github.panpf.sketch.decode.internal.ImageFormat
import com.github.panpf.sketch.decode.internal.newDecodeConfigByQualityParams
import com.github.panpf.sketch.decode.internal.supportBitmapRegionDecoder
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.request.get
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.source.FileDataSource
import com.github.panpf.sketch.util.Rect
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.util.toAndroidRect
import java.io.IOException

fun ComponentRegistry.Builder.supportVaultDecoder(): ComponentRegistry.Builder = apply {
    addDecoder(EncryptedBitmapFactoryDecoder.Factory())
    addDecoder(EncryptedVideoFrameDecoder.Factory())
}

open class EncryptedBitmapFactoryDecoder(
    requestContext: RequestContext,
    dataSource: DataSource,
) : HelperDecoder(
    requestContext = requestContext,
    dataSource = dataSource,
    decodeHelperFactory = { EncryptedBitmapFactoryDecodeHelper(requestContext.request, dataSource) }
) {

    class Factory : Decoder.Factory {

        override val key: String = "EncryptedBitmapFactoryDecoder"

        override fun create(
            requestContext: RequestContext,
            fetchResult: FetchResult
        ): Decoder? {
            val mimeType = requestContext.request.extras?.get("realMimeType") as String
            val dataSource = fetchResult.dataSource as? FileDataSource ?: return null
            return if (dataSource.path.toString().contains(BuildConfig.APPLICATION_ID) && mimeType.startsWith("image"))
                EncryptedBitmapFactoryDecoder(requestContext, dataSource)
            else null
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other != null && this::class == other::class
        }

        override fun hashCode(): Int {
            return this::class.hashCode()
        }

        override fun toString(): String = "EncryptedBitmapFactoryDecoder"
    }
}

/**
 * Decode encrypted bitmap using BitmapFactory
 */
@Throws(IOException::class)
private fun DataSource.decodeEncryptedBitmap(keychainHolder: KeychainHolder, options: BitmapFactory.Options? = null): Bitmap? {
    return with(this as FileDataSource) {
        val encryptedFile = path.toFile()
        val encryptedMedia = with(keychainHolder) {
            encryptedFile.decrypt<EncryptedMedia>()
        }
        BitmapFactory.decodeByteArray(
            encryptedMedia.bytes,
            0,
            encryptedMedia.bytes.size,
            options.apply { this?.outMimeType = encryptedMedia.mimeType }
        )
    }
}

/**
 * Use EncryptedBitmapRegionDecoder to decode part of a bitmap region
 */
@Throws(IOException::class)
private fun DataSource.decodeEncryptedRegionBitmap(
    keychainHolder: KeychainHolder,
    srcRect: android.graphics.Rect,
    options: BitmapFactory.Options? = null
): Bitmap? {
    return with(this as FileDataSource) {
        val encryptedFile = path.toFile()
        val encryptedMedia = with(keychainHolder) {
            encryptedFile.decrypt<EncryptedMedia>()
        }
        val regionDecoder = if (VERSION.SDK_INT >= VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(encryptedMedia.bytes, 0, encryptedMedia.bytes.size)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(encryptedMedia.bytes, 0, encryptedMedia.bytes.size, false)
        }
        try {
            regionDecoder.decodeRegion(srcRect, options.apply { this?.outMimeType = encryptedMedia.mimeType })
        } finally {
            regionDecoder.recycle()
        }
    }
}

@Throws(IOException::class)
fun DataSource.readEncryptedExifOrientation(keychainHolder: KeychainHolder): Int {
    return with(this as FileDataSource) {
        val encryptedFile = path.toFile()
        val encryptedMedia = with(keychainHolder) {
            encryptedFile.decrypt<EncryptedMedia>()
        }
        encryptedMedia.bytes.inputStream().use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        }
    }
}

private class EncryptedBitmapFactoryDecodeHelper(val request: ImageRequest, private val dataSource: DataSource) :
    DecodeHelper {

    private val keychainHolder = KeychainHolder(request.context)

    override val imageInfo: ImageInfo by lazy { decodeImageInfo() }
    override val supportRegion: Boolean by lazy {
        ImageFormat.parseMimeType(imageInfo.mimeType)?.supportBitmapRegionDecoder() == true
    }

    private val exifOrientation: Int by lazy { dataSource.readEncryptedExifOrientation(keychainHolder) }
    private val exifOrientationHelper by lazy { ExifOrientationHelper(exifOrientation) }

    override fun decode(sampleSize: Int): Image {
        val config = request.newDecodeConfigByQualityParams(imageInfo.mimeType).apply {
            inSampleSize = sampleSize
        }
        val options = config.toBitmapOptions()
        val bitmap = dataSource.decodeEncryptedBitmap(keychainHolder, options)
            ?: throw ImageInvalidException("Invalid image. decode return null")
        val image = bitmap.asSketchImage()
        val correctedImage = exifOrientationHelper.applyToImage(image) ?: image
        return correctedImage
    }

    override fun decodeRegion(region: Rect, sampleSize: Int): Image {
        val config = request.newDecodeConfigByQualityParams(imageInfo.mimeType).apply {
            inSampleSize = sampleSize
        }
        val options = config.toBitmapOptions()
        val originalRegion =
            exifOrientationHelper.applyToRect(region, imageInfo.size, reverse = true)
        val bitmap = dataSource.decodeEncryptedRegionBitmap(keychainHolder, originalRegion.toAndroidRect(), options)
            ?: throw ImageInvalidException("Invalid image. region decode return null")
        val image = bitmap.asSketchImage()
        val correctedImage = exifOrientationHelper.applyToImage(image) ?: image
        return correctedImage
    }

    private fun decodeImageInfo(): ImageInfo {
        val boundOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        dataSource.decodeEncryptedBitmap(keychainHolder, boundOptions)
        val mimeType = boundOptions.outMimeType
        val imageSize = Size(width = boundOptions.outWidth, height = boundOptions.outWidth)
        val correctedImageSize = exifOrientationHelper.applyToSize(imageSize)

        return ImageInfo(size = correctedImageSize, mimeType = mimeType)
    }

    override fun close() {

    }

    override fun toString(): String {
        return "EncryptedBitmapFactoryDecodeHelper(uri='${request.uri}', dataSource=$dataSource)"
    }
}