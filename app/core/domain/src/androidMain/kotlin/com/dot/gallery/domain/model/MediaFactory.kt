/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.dot.gallery.domain.reportError
import java.io.File
import kotlin.random.Random
import com.dot.gallery.domain.date.formatEpochSec

private const val TAG = "db.media"
private const val EXTENDED_DATE_FORMAT = "EEE, MMM d, yyyy"


fun Media.Companion.createFromUri(context: Context?, uri: Uri): UriMedia? {
    if (uri.path == null) return null
    val isContent = uri.scheme == ContentResolver.SCHEME_CONTENT

    val extension = uri.toString().substringAfterLast(".")
    // For content:// URIs the string has no usable extension, so prefer
    // the resolver's declared type (works for pending items via the grant).
    var mimeType: String? = if (isContent) {
        context?.contentResolver?.getType(uri)
    } else null
    if (mimeType == null) {
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension.lowercase()) {
                "apng" -> "image/apng"
                "jxl" -> "image/jxl"
                else -> null
            }
    }

    var duration: String? = null
    var timestamp = 0L
    var size = 0L
    var displayName: String? = null

    // Query the granted URI directly. A query on the specific URI returns
    // metadata even when the item is still IS_PENDING and owned by another
    // app (e.g. a freshly captured photo from a secure camera session),
    // because the camera grants us per-URI read access.
    if (isContent && context != null) {
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.MIME_TYPE,
            )
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putInt(
                        MediaStore.QUERY_ARG_MATCH_PENDING,
                        MediaStore.MATCH_INCLUDE
                    )
                }
                context.contentResolver.query(uri, projection, queryArgs, null)
            } else {
                context.contentResolver.query(uri, projection, null, null, null)
            }
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        .takeIf { it >= 0 }?.let { displayName = c.getString(it) }
                    c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                        .takeIf { it >= 0 }?.let { size = c.getLong(it) }
                    c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                        .takeIf { it >= 0 }?.let { timestamp = c.getLong(it) }
                    if (mimeType == null) {
                        c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                            .takeIf { it >= 0 }?.let { mimeType = c.getString(it) }
                    }
                }
            }
        } catch (e: Exception) {
            reportError(TAG, "Failed to query media metadata for $uri", e)
        }
    } else {
        uri.path?.let { File(it) }?.let {
            timestamp = try {
                it.lastModified()
            } catch (_: Exception) {
                0L
            }
        }
    }

    if (context != null) {
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                val hasVideo =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                val isVideo = "yes" == hasVideo
                if (isVideo) {
                    duration =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    if (timestamp == 0L) {
                        timestamp =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                                ?.toLongOrNull() ?: 0L
                    }
                }
                val originMimeType =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                if (mimeType == null) {
                    mimeType = if (isVideo) originMimeType else "image/*"
                }
            }
        } catch (e: Exception) {
            reportError(TAG, "Failed to extract media metadata for $uri", e)
        }
    }
    var formattedDate = ""
    if (timestamp != 0L) {
        formattedDate = timestamp.formatEpochSec(EXTENDED_DATE_FORMAT)
    }
    // Use the real MediaStore id for content URIs so the viewer can resolve
    // the item; fall back to a random id only for non-MediaStore URIs.
    val resolvedId: Long = if (isContent) {
        try {
            ContentUris.parseId(uri)
        } catch (_: Exception) {
            Random(kotlin.time.Clock.System.now().toEpochMilliseconds()).nextLong(-1000, 25600000)
        }
    } else {
        Random(kotlin.time.Clock.System.now().toEpochMilliseconds()).nextLong(-1000, 25600000)
    }
    val label = displayName ?: uri.toString().substringAfterLast("/")
    return UriMedia(
        id = resolvedId,
        label = label,
        uri = uri,
        path = uri.path.toString(),
        relativePath = uri.path.toString().substringBeforeLast("/"),
        albumID = -99L,
        albumLabel = "",
        timestamp = timestamp,
        fullDate = formattedDate,
        mimeType = mimeType ?: "null",
        duration = duration,
        favorite = 0,
        size = size,
        trashed = 0
    )
}
