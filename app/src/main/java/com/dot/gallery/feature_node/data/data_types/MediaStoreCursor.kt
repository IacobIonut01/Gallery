package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media

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

/**
 * only uri and projection will be used from MediaQuery
 */
fun ContentResolver.getCursor(
    mediaQuery: MediaQuery,
    queryArgs: Bundle,
): Cursor? {
    return query(mediaQuery.uri, mediaQuery.projection, queryArgs, null)
}


fun ContentResolver.getMediaUri(media: Media): Uri? {
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
    val bundle = Bundle().apply {
        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
    }
    val cursor = if (media.trashed == 1) getCursor(mediaQuery, bundle) else getCursor(mediaQuery)
    val uri = if (cursor != null && cursor.moveToFirst()) {
        ContentUris.withAppendedId(
            mediaQuery.uri,
            cursor.getLong(cursor.getColumnIndexOrThrow(mediaQuery.projection!!.first()))
        )
    } else null
    cursor?.close()
    return uri
}