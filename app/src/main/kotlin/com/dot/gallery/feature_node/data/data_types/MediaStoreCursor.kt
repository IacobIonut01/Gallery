/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MergeCursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.data.data_source.Query
import com.dot.gallery.feature_node.domain.model.ExifAttributes
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.getDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.IOException


suspend fun ContentResolver.query(
    mediaQuery: Query
): Cursor {
    return withContext(Dispatchers.IO) {
        return@withContext MergeCursor(
            arrayOf(
                query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    mediaQuery.projection,
                    mediaQuery.bundle,
                    null
                ),
                query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    mediaQuery.projection,
                    mediaQuery.bundle,
                    null
                )
            )
        )
    }
}

suspend fun ContentResolver.copyMedia(
    from: Media,
    path: String
) = withContext(Dispatchers.IO) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, from.label)
        put(MediaStore.MediaColumns.MIME_TYPE, from.mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, path)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val volumeUri =
        if (from.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    try {
        val outUri = insert(volumeUri, contentValues)
        if (outUri != null) {
            async {
                openFileDescriptor(outUri, "w", null).use { target ->
                    openFileDescriptor(from.uri, "r").use { from ->
                        if (target != null && from != null) {
                            try {
                                FileUtils.copy(from.fileDescriptor, target.fileDescriptor)
                            } catch (e: IOException) {
                                if (e.message.toString().contains("ENOSPC")) {
                                    Log.e(Constants.TAG, "No space left on device")
                                } else {
                                    Log.e(Constants.TAG, e.message.toString())
                                }
                                return@async
                            }
                        }
                    }
                }
            }.await()
            val updatedValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
                put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis())
            }

            update(
                outUri,
                updatedValues,
                null
            ) > 0
        } else false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


fun ContentResolver.overrideImage(
    uri: Uri,
    bitmap: Bitmap,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    mimeType: String = "image/png",
    relativePath: String = Environment.DIRECTORY_PICTURES,
    displayName: String
): Boolean {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    return runCatching {
        openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(format, 95, stream))
                throw IOException("Failed to save bitmap.")
        } ?: throw IOException("Failed to open output stream.")
        update(uri, values, null) > 0 && update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null) > 0
    }.getOrElse {
        throw it
    }
}

fun ContentResolver.saveImage(
    bitmap: Bitmap,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    mimeType: String = "image/png",
    relativePath: String = Environment.DIRECTORY_PICTURES,
    displayName: String
): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            if (relativePath.contains("DCIM") || relativePath.contains("Pictures")) relativePath
            else Environment.DIRECTORY_PICTURES + "/Edited"
        )
    }

    var uri: Uri? = null

    return runCatching {
        insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
            uri = it // Keep uri reference so it can be removed on failure

            openOutputStream(it)?.use { stream ->
                if (!bitmap.compress(format, 95, stream))
                    throw IOException("Failed to save bitmap.")
            } ?: throw IOException("Failed to open output stream.")

        } ?: throw IOException("Failed to create new MediaStore record.")
    }.getOrElse {
        uri?.let { orphanUri ->
            // Don't leave an orphan entry in the MediaStore
            delete(orphanUri, null, null)
        }

        return null
    }
}

suspend fun ContentResolver.updateMedia(
    media: Media,
    contentValues: ContentValues
): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        update(
            media.uri,
            contentValues,
            null
        ) > 0
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        false
    }
}

suspend fun ContentResolver.updateMediaExif(
    media: Media,
    exifAttributes: ExifAttributes
) = withContext(Dispatchers.IO) {
    return@withContext try {
        openFileDescriptor(media.uri, "rw").use { imagePfd ->
            if (imagePfd != null) {
                val exif = ExifInterface(imagePfd.fileDescriptor)
                exifAttributes.writeExif(exif)
                runCatching {
                    exif.saveAttributes()
                }.onFailure {
                    it.printStackTrace()
                }
            }
            true
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        false
    }
}


@Throws(Exception::class)
fun Cursor.getMediaFromCursor(): Media {
    val id: Long =
        getLong(getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
    val path: String =
        getString(getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
    val relativePath: String =
        getString(getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
    val title: String =
        getString(getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
    val albumID: Long =
        getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID))
    val albumLabel: String = try {
        getString(getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME))
    } catch (_: Exception) {
        Build.MODEL
    }
    val takenTimestamp: Long? = try {
        getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN))
    } catch (_: Exception) {
        null
    }
    val modifiedTimestamp: Long =
        getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
    val duration: String? = try {
        getString(getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION))
    } catch (_: Exception) {
        null
    }
    val orientation: Int =
        getInt(getColumnIndexOrThrow(MediaStore.MediaColumns.ORIENTATION))
    val mimeType: String =
        getString(getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
    val isFavorite: Int =
        getInt(getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE))
    val isTrashed: Int =
        getInt(getColumnIndexOrThrow(MediaStore.MediaColumns.IS_TRASHED))
    val expiryTimestamp: Long? = try {
        getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES))
    } catch (_: Exception) {
        null
    }
    val contentUri = if (mimeType.contains("image"))
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    else
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val uri = ContentUris.withAppendedId(contentUri, id)
    val formattedDate = modifiedTimestamp.getDate(Constants.FULL_DATE_FORMAT)
    return Media(
        id = id,
        label = title,
        uri = uri,
        path = path,
        relativePath = relativePath,
        albumID = albumID,
        albumLabel = albumLabel,
        timestamp = modifiedTimestamp,
        takenTimestamp = takenTimestamp,
        expiryTimestamp = expiryTimestamp,
        fullDate = formattedDate,
        duration = duration,
        favorite = isFavorite,
        trashed = isTrashed,
        orientation = orientation,
        mimeType = mimeType
    )
}