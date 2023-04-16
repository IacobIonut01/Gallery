/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.repository

import android.media.MediaScannerConnection
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    fun getMedia(): Flow<List<Media>>

    fun getFavorites(mediaOrder: MediaOrder): Flow<List<Media>>

    fun getTrashed(mediaOrder: MediaOrder): Flow<List<Media>>

    fun getAlbums(mediaOrder: MediaOrder): Flow<List<Album>>

    suspend fun insertMedia(media: Media)

    suspend fun getMediaById(mediaId: Long): Media?

    fun getMediaByAlbumId(albumId: Long): Flow<List<Media>>

    fun getMediaByUri(uriAsString: String): Flow<List<Media>>

    suspend fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean
    )

    suspend fun trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        trash: Boolean
    )

    suspend fun deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>
    )

}