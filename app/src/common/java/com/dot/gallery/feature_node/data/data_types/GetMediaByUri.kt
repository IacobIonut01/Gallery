package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.Query.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media

fun ContentResolver.getMediaByUri(uri: Uri): Media? {
    var media: Media? = null
    val mediaQuery = MediaQuery().copy(
        bundle = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                MediaStore.MediaColumns.DATA + "=?"
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(uri.toString())
            )
        }
    )
    with(query(mediaQuery)) {
        moveToFirst()
        while (!isAfterLast) {
            try {
                media = getMediaFromCursor()
                break
            } catch (e: Exception) {
                close()
                e.printStackTrace()
            }
        }
        moveToNext()
        close()
    }

    return media
}