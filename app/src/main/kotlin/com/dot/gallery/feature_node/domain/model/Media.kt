/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model

import android.net.Uri
import android.os.Parcelable
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Immutable
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.presentation.util.getDate
import kotlinx.parcelize.Parcelize
import java.io.File

@Immutable
@Parcelize
data class Media(
    val id: Long = 0,
    val label: String,
    val uri: Uri,
    val path: String,
    val albumID: Long,
    val albumLabel: String,
    val timestamp: Long,
    val takenTimestamp: Long? = null,
    val fullDate: String,
    val mimeType: String,
    val orientation: Int,
    val favorite: Int,
    val trashed: Int,
    val duration: String? = null,
) : Parcelable {

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
    fun readUriOnly(): Boolean = albumID == -99L && albumLabel == ""
    companion object {
        fun createFromUri(uri: Uri): Media? {
            if (uri.path == null) return null
            val extension = uri.toString().substringAfterLast(".")
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).toString()
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
                label = uri.toString().substringAfterLast("/"),
                uri = uri,
                path = uri.path.toString(),
                albumID = -99L,
                albumLabel = "",
                timestamp = timestamp,
                fullDate = formattedDate,
                mimeType = mimeType,
                favorite = 0,
                trashed = 0,
                orientation = 0
            )
        }
    }
}
