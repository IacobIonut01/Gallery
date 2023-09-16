/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.Query.TrashQuery
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun ContentResolver.getMediaTrashed(): List<Media> {
    return withContext(Dispatchers.IO) {
        val mediaQuery = TrashQuery().copy(
            bundle = Bundle().apply {
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.MediaColumns.DATE_EXPIRES)
                )
                putInt(
                    ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            }
        )
        return@withContext MediaOrder.Expiry().sortMedia(getMedia(mediaQuery))
    }
}

