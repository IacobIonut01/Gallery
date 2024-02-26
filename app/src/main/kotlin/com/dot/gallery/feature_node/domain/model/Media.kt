/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Parcelable
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import coil3.compose.EqualityDelegate
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.presentation.util.getDate
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File
import kotlin.random.Random

@Immutable
@Parcelize
data class Media(
    val id: Long = 0,
    val label: String,
    val uri: Uri,
    val path: String,
    val relativePath: String,
    val albumID: Long,
    val albumLabel: String,
    val timestamp: Long,
    val expiryTimestamp: Long? = null,
    val takenTimestamp: Long? = null,
    val fullDate: String,
    val mimeType: String,
    val favorite: Int,
    val trashed: Int,
    val duration: String? = null,
) : Parcelable {

    @IgnoredOnParcel
    @Stable
    val isVideo: Boolean = mimeType.startsWith("video/") && duration != null

    @IgnoredOnParcel
    @Stable
    val isImage: Boolean = mimeType.startsWith("image/")

    @IgnoredOnParcel
    @Stable
    val isTrashed: Boolean = trashed == 1

    @IgnoredOnParcel
    @Stable
    val isFavorite: Boolean = favorite == 1

    @Stable
    override fun toString(): String {
        return "$id, $path, $fullDate, $mimeType, favorite=$favorite"
    }

    /**
     * Used to determine if the Media object is not accessible
     * via MediaStore.
     * This happens when the user tries to open media from an app
     * using external sources (in our case, Gallery Media Viewer), but
     * the specific media is only available internally in that app
     * (Android/data(OR media)/com.package.name/)
     *
     * If it's readUriOnly then we know that we should expect a barebone
     * Media object with limited functionality (no favorites, trash, timestamp etc)
     */
    @IgnoredOnParcel
    @Stable
    val readUriOnly: Boolean = albumID == -99L && albumLabel == ""

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

    @IgnoredOnParcel
    @Stable
    val volume: String = path.substringBeforeLast("/").removeSuffix(relativePath.removeSuffix("/"))

    companion object {
        fun createFromUri(context: Context, uri: Uri): Media? {
            if (uri.path == null) return null
            val extension = uri.toString().substringAfterLast(".")
            var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).toString()
            var duration: String? = null
            try {
                val retriever = MediaMetadataRetriever().apply {
                    setDataSource(context, uri)
                }
                val hasVideo =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                val isVideo = "yes" == hasVideo
                if (isVideo) {
                    duration =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                }
                if (mimeType.isEmpty()) {
                    mimeType = if (isVideo) "video/*" else "image/*"
                }
            } catch (_: Exception) {
            }
            var timestamp = 0L
            uri.path?.let { File(it) }?.let {
                timestamp = try {
                    it.lastModified()
                } catch (_: Exception) {
                    0L
                }
            }
            var formattedDate = ""
            if (timestamp != 0L) {
                formattedDate = timestamp.getDate(Constants.EXTENDED_DATE_FORMAT)
            }
            return Media(
                id = Random(System.currentTimeMillis()).nextLong(-1000, 25600000),
                label = uri.toString().substringAfterLast("/"),
                uri = uri,
                path = uri.path.toString(),
                relativePath = uri.path.toString().substringBeforeLast("/"),
                albumID = -99L,
                albumLabel = "",
                timestamp = timestamp,
                fullDate = formattedDate,
                mimeType = mimeType,
                duration = duration,
                favorite = 0,
                trashed = 0
            )
        }
    }
}

/**
 * Since the media object is stable, we can use a custom equality delegate
 * to avoid the default equals and hashCode implementation and ensure that
 * the object is always considered equal to another object of the same type
 * regardless of its properties.
 * This is avoiding unnecessary recompositions of the painter in the MediaImage
 */
class MediaEqualityDelegate : EqualityDelegate {
    override fun equals(self: Any?, other: Any?): Boolean = true

    override fun hashCode(self: Any?): Int = 31
}