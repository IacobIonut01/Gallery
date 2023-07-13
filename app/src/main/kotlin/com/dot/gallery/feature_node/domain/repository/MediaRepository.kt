/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.repository

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.PinnedAlbum
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    fun getMedia(): Flow<Resource<List<Media>>>

    fun getMediaByType(allowedMedia: AllowedMedia): Flow<Resource<List<Media>>>

    fun getFavorites(mediaOrder: MediaOrder): Flow<Resource<List<Media>>>

    fun getTrashed(mediaOrder: MediaOrder): Flow<Resource<List<Media>>>

    fun getAlbums(mediaOrder: MediaOrder): Flow<Resource<List<Album>>>

    suspend fun insertPinnedAlbum(pinnedAlbum: PinnedAlbum)

    suspend fun removePinnedAlbum(pinnedAlbum: PinnedAlbum)

    suspend fun getMediaById(mediaId: Long): Media?

    fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>>

    fun getMediaByAlbumIdWithType(albumId: Long, allowedMedia: AllowedMedia): Flow<Resource<List<Media>>>

    fun getAlbumsWithType(allowedMedia: AllowedMedia): Flow<Resource<List<Album>>>

    fun getMediaByUri(uriAsString: String, isSecure: Boolean): Flow<Resource<List<Media>>>

    fun getMediaListByUris(listOfUris: List<Uri>): Flow<Resource<List<Media>>>

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