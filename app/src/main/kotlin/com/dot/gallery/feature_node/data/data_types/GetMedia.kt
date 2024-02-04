/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import com.dot.gallery.feature_node.data.data_source.Query
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun ContentResolver.getMedia(
    mediaQuery: Query = Query.MediaQuery(),
    mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
): List<Media> {
    return withContext(Dispatchers.IO) {
        val timeStart = System.currentTimeMillis()
        val media = ArrayList<Media>()
        query(mediaQuery).use { cursor ->
            while (cursor.moveToNext()) {
                try {
                    media.add(cursor.getMediaFromCursor())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return@withContext mediaOrder.sortMedia(media).also {
            println("Media parsing took: ${System.currentTimeMillis() - timeStart}ms")
        }
    }
}

suspend fun ContentResolver.findMedia(mediaQuery: Query): Media? {
    return withContext(Dispatchers.IO) {
        val mediaList = getMedia(mediaQuery)
        return@withContext if (mediaList.isEmpty()) null else mediaList.first()
    }
}