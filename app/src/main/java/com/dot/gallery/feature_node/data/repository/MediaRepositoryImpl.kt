package com.dot.gallery.feature_node.data.repository

import android.media.MediaScannerConnection
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.data.data_source.MediaParser
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class MediaRepositoryImpl (
    private val mediaParser: MediaParser
): MediaRepository {
    override suspend fun getMedia(): Flow<Resource<List<Media>>> {
        return mediaParser.getMedia()
    }

    override suspend fun getAlbums(): Flow<Resource<List<Album>>> {
        return mediaParser.getAlbum()
    }

    override suspend fun insertMedia(media: Media, callback: MediaScannerConnection.OnScanCompletedListener) {
        mediaParser.insertMedia(media, callback)
    }

    override suspend fun getMediaById(mediaId: Long): Media? {
        return mediaParser.getMediaById(mediaId)
    }

    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>> {
        return mediaParser.getMediaByAlbumId(albumId)
    }

    override suspend fun deleteMedia(mediaId: Long) {
        mediaParser.deleteMedia(mediaId)
    }

    override suspend fun deleteMedia(media: Media) {
        mediaParser.deleteMedia(media)
    }
}