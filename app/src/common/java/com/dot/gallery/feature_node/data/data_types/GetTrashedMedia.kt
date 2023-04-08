package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.Query.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType

fun ContentResolver.getMediaTrashed(
    mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
): List<Media> {
    val mediaQuery = MediaQuery().copy(
        bundle = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }
    )
    return mediaOrder.sortMedia(getMedia(mediaQuery))
}

