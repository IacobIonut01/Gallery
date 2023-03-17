package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import com.dot.gallery.feature_node.data.data_source.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType

fun ContentResolver.getMediaByType(mediaQuery: MediaQuery): List<Media> {
    val media = ArrayList<Media>()
    getCursor(mediaQuery) { cursor ->
        cursor.getMediaFromCursor(mediaQuery.uri)?.let { media.add(it) }
    }
    return media
}

fun ContentResolver.getMedia(
    mediaQueries: List<MediaQuery>? = null,
    mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
): List<Media> {
    val media = ArrayList<Media>().also {
        if (mediaQueries == null) {
            // Add all media by default
            it.addAll(getImages(mediaOrder))
            it.addAll(getVideos(mediaOrder))
        } else {
            for (query in mediaQueries) {
                it.addAll(getMediaByType(query))
            }
        }
    }
    return mediaOrder.sortMedia(media)
}

fun ContentResolver.findMedia(
    mediaQueries: List<MediaQuery>? = null,
): Media? {
    val mediaList = getMedia(mediaQueries)
    return if (mediaList.isEmpty()) null else mediaList.first()
}