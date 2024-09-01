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
import androidx.room.Entity
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.presentation.util.getDate
import kotlinx.parcelize.Parcelize
import java.io.File
import kotlin.random.Random

@Immutable
@Parcelize
@Entity(tableName = "media", primaryKeys = ["id"])
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
    val size: Long,
    val duration: String? = null,
) : Parcelable {

    @Stable
    override fun toString(): String {
        return "$id, $path, $fullDate, $mimeType, favorite=$favorite"
    }

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
                size = 0,
                trashed = 0
            )
        }
    }
}

/**
 * Determine if the current media is a raw format
 *
 * Checks if [Media.mimeType] starts with "image/x-" or "image/vnd."
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
val Media.isRaw: Boolean get() =
    mimeType.isNotBlank() && (mimeType.startsWith("image/x-") || mimeType.startsWith("image/vnd."))

val Media.fileExtension: String get() = label.substringAfterLast(".").removePrefix(".")

val Media.volume: String get() = path.substringBeforeLast("/").removeSuffix(relativePath.removeSuffix("/"))

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
val Media.readUriOnly: Boolean get() = albumID == -99L && albumLabel == ""

val Media.isVideo: Boolean get() = mimeType.startsWith("video/") && duration != null

val Media.isImage: Boolean get() = mimeType.startsWith("image/")

val Media.isTrashed: Boolean get() = trashed == 1

val Media.isFavorite: Boolean get() = favorite == 1
