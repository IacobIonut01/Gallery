package com.dot.gallery.core.decoder.glide

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.dot.gallery.core.decoder.format.TiffImageDecoder

/**
 * TIFF decoder for the MimeInputStream (content Uri) path. Delegates to [TiffImageDecoder] which
 * uses platform ImageDecoder (API 28+) with a BigTIFF / oversized EXIF-thumbnail fallback.
 */
class TiffMimeInputStreamDecoder(
    private val bitmapPool: BitmapPool
): ResourceDecoder<MimeInputStream, Bitmap> {

    override fun handles(source: MimeInputStream, options: Options): Boolean =
        TiffMime.isTiff(source.mimeType)

    @RequiresApi(Build.VERSION_CODES.P)
    override fun decode(
        source: MimeInputStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Bitmap>? {
        val data = source.inputStream.readBytes()
        val bmp = TiffImageDecoder.decode(data, width, height) ?: return null
        return BitmapResource.obtain(bmp, bitmapPool)
    }
}
