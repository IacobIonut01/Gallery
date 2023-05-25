/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.Query.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun ContentResolver.getMediaByUri(uri: Uri): Media? {
    return withContext(Dispatchers.IO) {
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
        if (media == null) {
            media = Media.createFromUri(uri)
        }

        return@withContext media
    }
}

suspend fun ContentResolver.getMediaListByUris(list: List<Uri>): List<Media> {
    return withContext(Dispatchers.IO) {
        val mediaList = ArrayList<Media>()
        val mediaQuery = MediaQuery().copy(
            bundle = Bundle().apply {
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    MediaStore.MediaColumns.DATA + "=?"
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    list.map { it.toString() }.toTypedArray()
                )
            }
        )
        with(query(mediaQuery)) {
            moveToFirst()
            while (!isAfterLast) {
                try {
                    mediaList.add(getMediaFromCursor())
                    break
                } catch (e: Exception) {
                    close()
                    e.printStackTrace()
                }
            }
            moveToNext()
            close()
        }
        if (mediaList.isEmpty()) {
            for (uri in list) {
                Media.createFromUri(uri)?.let { mediaList.add(it) }
            }
        }
        mediaList
    }
}