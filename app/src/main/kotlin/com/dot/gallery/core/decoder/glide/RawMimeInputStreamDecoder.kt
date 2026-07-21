package com.dot.gallery.core.decoder.glide

import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.dot.gallery.core.decoder.NativeRawDecoder
import com.dot.gallery.core.decoder.format.TiffImageDecoder

/**
 * Camera-RAW decoder for the MimeInputStream (content Uri) path, used by the timeline grid and
 * album covers. Android has no native decoder for CR2/NEF/ARW/DNG/ORF/PEF/RW2/SRW/… so without
 * this Glide fails with a `DecodePath` error for every local RAW asset. Most RAW formats are
 * TIFF-based with an embedded JPEG preview, which [TiffImageDecoder.decodePreview] extracts
 * (falling back to the EXIF thumbnail for non-TIFF RAW). Mirrors the Sketch `SketchRawDecoder`.
 */
class RawMimeInputStreamDecoder(
    private val bitmapPool: BitmapPool
) : ResourceDecoder<MimeInputStream, Bitmap> {

    override fun handles(source: MimeInputStream, options: Options): Boolean =
        RawMime.isCameraRaw(source.mimeType)

    override fun decode(
        source: MimeInputStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Bitmap>? {
        val data = source.inputStream.readBytes()
        // Prefer the embedded JPEG preview (fastest); fall back to LibRaw's decoded thumbnail for
        // non-TIFF RAW (RAF/CRW/X3F) that has no extractable embedded JPEG.
        val bmp = TiffImageDecoder.decodePreview(data, width, height)
            ?: NativeRawDecoder.getThumbnail(data)
            ?: return null
        return BitmapResource.obtain(bmp, bitmapPool)
    }
}

/**
 * Camera-RAW MIME registry, mirroring [com.dot.gallery.feature_node.domain.util.isRaw]
 * (`image/x-*` / `image/vnd.*`). Excludes PSD (own decoder) and BMP variants (natively decodable).
 */
object RawMime {
    fun isCameraRaw(mime: String?): Boolean {
        if (mime.isNullOrEmpty()) return false
        val m = mime.lowercase().substringBefore(';').trim()
        if (m == "image/vnd.adobe.photoshop") return false
        if (m == "image/x-ms-bmp" || m == "image/x-bmp" || m == "image/x-windows-bmp") return false
        return m.startsWith("image/x-") || m.startsWith("image/vnd.")
    }
}
