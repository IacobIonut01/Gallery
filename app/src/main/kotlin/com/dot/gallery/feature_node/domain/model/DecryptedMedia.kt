package com.dot.gallery.feature_node.domain.model

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Stable
@Parcelize
@kotlinx.serialization.Serializable
data class DecryptedMedia(
    val id: Long = 0,
    val label: String,
    val uri: String,
    val path: String,
    val timestamp: Long,
    val mimeType: String,
    val duration: String? = null,
) : Parcelable, Serializable {

    @IgnoredOnParcel
    @Stable
    val isVideo: Boolean = mimeType.startsWith("video/") && duration != null

    @IgnoredOnParcel
    @Stable
    val isImage: Boolean = mimeType.startsWith("image/")

    @Stable
    override fun toString(): String {
        return "$id, $path, $timestamp, $mimeType"
    }

    /**
     * Determine if the current media is a raw format
     *
     * Checks if [mimeType] starts with "image/x-" or "image/vnd."
     *
     * Most used formats:
     * - ARW: image/x-sony-arw
     * - CR2: image/x-canon-cr2
     * - CRW: image/x-canon-crw
     * - DCR: image/x-kodak-dcr
     * - DNG: image/x-adobe-dng
     * - ERF: image/x-epson-erf
     * - K25: image/x-kodak-k25
     * - KDC: image/x-kodak-kdc
     * - MRW: image/x-minolta-mrw
     * - NEF: image/x-nikon-nef
     * - ORF: image/x-olympus-orf
     * - PEF: image/x-pentax-pef
     * - RAF: image/x-fuji-raf
     * - RAW: image/x-panasonic-raw
     * - SR2: image/x-sony-sr2
     * - SRF: image/x-sony-srf
     * - X3F: image/x-sigma-x3f
     *
     * Other proprietary image types in the standard:
     * image/vnd.manufacturer.filename_extension for instance for NEF by Nikon and .mrv for Minolta:
     * - NEF: image/vnd.nikon.nef
     * - Minolta: image/vnd.minolta.mrw
     */
    @IgnoredOnParcel
    @Stable
    val isRaw: Boolean =
        mimeType.isNotBlank() && (mimeType.startsWith("image/x-") || mimeType.startsWith("image/vnd."))

    @IgnoredOnParcel
    @Stable
    val fileExtension: String = label.substringAfterLast(".").removePrefix(".")

    companion object {

        fun EncryptedMedia.asDecryptedMedia(uri: Uri): DecryptedMedia {
            return DecryptedMedia(
                id = id,
                label = label,
                uri = uri.toString(),
                path = path,
                timestamp = timestamp,
                mimeType = mimeType,
                duration = duration
            )
        }
    }

}

fun DecryptedMedia.compatibleMimeType(): String {
    return if (isImage) when(mimeType) {
        "image/jpeg" -> "image/jpeg"
        "image/png" -> "image/png"
        else -> "image/png"
    } else mimeType
}

fun DecryptedMedia.compatibleBitmapFormat(): Bitmap.CompressFormat {
    return when(mimeType) {
        "image/jpeg" -> Bitmap.CompressFormat.JPEG
        "image/png" -> Bitmap.CompressFormat.PNG
        else -> Bitmap.CompressFormat.PNG
    }
}