package com.dot.gallery.feature_node.data.repository

import android.media.MediaScannerConnection
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.data.data_source.MediaParser
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class MediaRepositoryImpl (
    private val dao: MediaParser
): MediaRepository {
    override fun getMedia(): Flow<Resource<List<Media>>> {
        return dao.getMedia()
    }

    override suspend fun insertMedia(media: Media, callback: MediaScannerConnection.OnScanCompletedListener) {
        dao.insertMedia(media, callback)
    }

    override suspend fun getMediaById(mediaId: Long): Media? {
        return dao.getMediaById(mediaId)
    }

    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>> {
        return dao.getMediaByAlbumId(albumId)
    }

    override suspend fun deleteMedia(mediaId: Long) {
        dao.deleteMedia(mediaId)
    }

    override suspend fun deleteMedia(media: Media) {
        dao.deleteMedia(media)
    }
}