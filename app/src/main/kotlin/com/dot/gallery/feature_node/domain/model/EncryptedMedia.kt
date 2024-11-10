/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInput
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

@Immutable
@Parcelize
@kotlinx.serialization.Serializable
data class EncryptedMedia(
    val id: Long = 0,
    val label: String,
    val bytes: ByteArray,
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedMedia

        if (id != other.id) return false
        if (label != other.label) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (path != other.path) return false
        if (timestamp != other.timestamp) return false
        if (mimeType != other.mimeType) return false
        if (duration != other.duration) return false
        if (isVideo != other.isVideo) return false
        if (isImage != other.isImage) return false
        if (isRaw != other.isRaw) return false
        if (fileExtension != other.fileExtension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (duration?.hashCode() ?: 0)
        result = 31 * result + isVideo.hashCode()
        result = 31 * result + isImage.hashCode()
        result = 31 * result + isRaw.hashCode()
        result = 31 * result + fileExtension.hashCode()
        return result
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

}

fun Media.toEncryptedMedia(bytes: ByteArray): EncryptedMedia {
    return EncryptedMedia(
        id = id,
        label = label,
        bytes = bytes,
        path = path,
        timestamp = timestamp,
        mimeType = mimeType,
        duration = duration
    )
}

@Suppress("UNCHECKED_CAST")
fun <T : Serializable> fromByteArray(byteArray: ByteArray): T {
    val byteArrayInputStream = ByteArrayInputStream(byteArray)
    val objectInput: ObjectInput = ObjectInputStream(byteArrayInputStream)
    val result = objectInput.readObject() as T
    objectInput.close()
    byteArrayInputStream.close()
    return result
}

fun Serializable.toByteArray(): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
    objectOutputStream.writeObject(this)
    objectOutputStream.flush()
    val result = byteArrayOutputStream.toByteArray()
    byteArrayOutputStream.close()
    objectOutputStream.close()
    return result
}
