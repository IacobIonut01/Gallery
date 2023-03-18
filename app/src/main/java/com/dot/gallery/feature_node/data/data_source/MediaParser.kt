package com.dot.gallery.feature_node.data.data_source

import android.media.MediaScannerConnection
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.flow.Flow

interface MediaParser {

    suspend fun getMedia(): Flow<Resource<List<Media>>>
    suspend fun getAlbum(): Flow<Resource<List<Album>>>

    suspend fun insertMedia(media: Media, callback: MediaScannerConnection.OnScanCompletedListener)

    suspend fun getMediaById(mediaId: Long): Media?

    fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>>

    suspend fun deleteMedia(mediaId: Long)

    suspend fun deleteMedia(media: Media)

}