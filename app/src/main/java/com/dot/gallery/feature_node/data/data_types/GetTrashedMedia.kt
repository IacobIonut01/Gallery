package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType

fun ContentResolver.getMediaTrashed(
    mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
): List<Media> {
    val mediaQueries = arrayListOf(
        MediaQuery.PhotoQuery(),
        MediaQuery.VideoQuery()
    )
    val bundle = Bundle().apply {
        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
    }
    val media = ArrayList<Media>().apply {
        for (query in mediaQueries) {
            addAll(getMediaByType(query, bundle))
        }
    }
    return mediaOrder.sortMedia(media)
}

