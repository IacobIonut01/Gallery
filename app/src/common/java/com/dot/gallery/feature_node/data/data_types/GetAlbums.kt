/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.Query
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun ContentResolver.getAlbums(mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)): List<Album> {
    return withContext(Dispatchers.IO) {
        val albums = ArrayList<Album>()
        val albumQuery = Query.AlbumQuery().copy(
            bundle = Bundle().apply {
                putInt(
                    MediaStore.QUERY_ARG_MATCH_TRASHED,
                    MediaStore.MATCH_EXCLUDE
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.MediaColumns.DATE_MODIFIED)
                )
            },
        )
        with(query(albumQuery)) {
            moveToFirst()
            while (!isAfterLast) {
                try {
                    val id = getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID))
                    val label: String? = try {
                        getString(getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME))
                    } catch (e: Exception) {
                        Build.MODEL
                    }
                    val thumbnailPath =
                        getString(getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                    val thumbnailDate =
                        getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                    val album =
                        Album(id, label ?: Build.MODEL, thumbnailPath, thumbnailDate, count = 1)
                    val currentAlbum = albums.find { albm -> albm.id == id }
                    if (currentAlbum == null)
                        albums.add(album)
                    else {
                        val i = albums.indexOf(currentAlbum)
                        albums[i].count++
                        if (albums[i].timestamp <= thumbnailDate) {
                            album.count = albums[i].count
                            albums[i] = album
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                moveToNext()
            }
            close()
        }
        return@withContext mediaOrder.sortAlbums(albums)
    }
}