package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import com.dot.gallery.feature_node.data.data_source.Query
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType

fun ContentResolver.getMedia(
    mediaQuery: Query = Query.MediaQuery(),
    mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
): List<Media> {
    val media = ArrayList<Media>()
    with(query(mediaQuery)) {
        moveToFirst()
        while (!isAfterLast) {
            try {
                media.add(getMediaFromCursor())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            moveToNext()
        }
        close()
    }
    return mediaOrder.sortMedia(media)
}

fun ContentResolver.findMedia(mediaQuery: Query): Media? {
    val mediaList = getMedia(mediaQuery)
    return if (mediaList.isEmpty()) null else mediaList.first()
}