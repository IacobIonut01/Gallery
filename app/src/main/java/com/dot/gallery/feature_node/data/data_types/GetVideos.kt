package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType

fun ContentResolver.getVideos(mediaOrder: MediaOrder): List<Media> {
    val sortBy = when (mediaOrder) {
        is MediaOrder.Date -> MediaStore.Video.Media.DATE_MODIFIED
        is MediaOrder.Label -> MediaStore.Video.Media.TITLE
    }
    val sortType = when (mediaOrder.orderType) {
        OrderType.Ascending -> "ASC"
        OrderType.Descending -> "DESC"
    }
    val videoQuery = MediaQuery.VideoQuery().copy(
        sortOrder = "$sortBy $sortType"
    )
    return getMediaByType(videoQuery)
}
