package com.dot.gallery.core.decoder.glide

/**
 * Camera-RAW MIME registry, mirroring [com.dot.gallery.domain.util.isRaw]
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
