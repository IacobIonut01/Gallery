package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media

fun ContentResolver.deleteMedia(media: Media): Boolean {
    var deleted = false
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
    getCursor(mediaQuery)?.let {
        if (it.moveToFirst()) {
            val deleteUri = ContentUris.withAppendedId(
                mediaQuery.uri, it.getLong(it.getColumnIndexOrThrow(mediaQuery.projection!!.first())))
            deleted = delete(deleteUri, null, null) > 0
        }
        it.close()
    }
    return deleted
}