package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.dot.gallery.feature_node.data.data_source.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media
import java.io.File

fun Cursor.getMediaFromCursor(uri: Uri): Media? {
    val isVideo = uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val id: Long
    val path: String?
    val title: String?
    val albumID: Long
    val timestamp: Long
    val duration: String?
    if (isVideo) {
        id = getLong(getColumnIndexOrThrow(MediaStore.Video.Media._ID))
        path = getString(getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
        title = getString(getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
        albumID = getLong(getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
        timestamp =
            getLong(getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED))
        duration = getString(getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
    } else {
        id = getLong(getColumnIndexOrThrow(MediaStore.Images.Media._ID))
        path = getString(getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        title = getString(getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
        albumID = getLong(getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
        timestamp =
            getLong(getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
        duration = null
    }
    if (path != null && title != null) {
        return Media(
            id = id,
            label = title,
            uri = Uri.fromFile(File(path)),
            path = path,
            albumID = albumID,
            timestamp = timestamp,
            duration = duration
        )
    }
    return null
}

fun ContentResolver.getCursor(
    mediaQuery: MediaQuery,
    callback: (Cursor) -> Unit
) {
    query(
        mediaQuery.uri,
        mediaQuery.projection,
        mediaQuery.selection,
        mediaQuery.selectionArgs,
        mediaQuery.sortOrder
    )?.let {
        while (it.moveToNext()) {
            try {
                callback.invoke(it)
            } catch (e: Exception) {
                e.printStackTrace()
                it.close()
                Log.d("GalleryException", e.message.toString())
            }
            it.moveToNext()
        }
        it.close()
    }
}

fun ContentResolver.getCursor(
    mediaQuery: MediaQuery
): Cursor? {
    return query(
        mediaQuery.uri,
        mediaQuery.projection,
        mediaQuery.selection,
        mediaQuery.selectionArgs,
        mediaQuery.sortOrder
    )
}

fun ContentResolver.getMediaDeleteUri(media: Media): Uri? {
    val mediaQuery = if (media.duration == null) {
        MediaQuery.PhotoQuery().copy(
            projection = arrayOf(MediaStore.Images.Media._ID),
            selection = MediaStore.Images.Media.DATA + " = ?",
            selectionArgs = arrayOf(media.path)
        )
    } else {
        MediaQuery.VideoQuery().copy(
            projection = arrayOf(MediaStore.Video.Media._ID),
            selection = MediaStore.Video.Media.DATA + " = ?",
            selectionArgs = arrayOf(media.path)
        )
    }

    val cursor = getCursor(mediaQuery)
    val uri = if (cursor != null && cursor.moveToFirst()) {
        ContentUris.withAppendedId(
            mediaQuery.uri,
            cursor.getLong(cursor.getColumnIndexOrThrow(mediaQuery.projection!!.first()))
        )
    } else null
    cursor?.close()
    return uri
}