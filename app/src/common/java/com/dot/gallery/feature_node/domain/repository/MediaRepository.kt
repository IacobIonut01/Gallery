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

    suspend fun getMedia(): Flow<Resource<List<Media>>>

    suspend fun getFavorites(mediaOrder: MediaOrder): Flow<Resource<List<Media>>>

    suspend fun getTrashed(mediaOrder: MediaOrder): Flow<Resource<List<Media>>>

    suspend fun getAlbums(mediaOrder: MediaOrder): Flow<Resource<List<Album>>>

    suspend fun insertMedia(media: Media, callback: MediaScannerConnection.OnScanCompletedListener)

    suspend fun getMediaById(mediaId: Long): Media?

    fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>>

    fun getMediaByUri(uriAsString: String): Flow<Resource<List<Media>>>

    suspend fun toggleFavorite(media: Media): Int

    suspend fun toggleFavorite(mediaList: List<Media>)

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