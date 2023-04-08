package com.dot.gallery.feature_node.data.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.data.data_source.Query
import com.dot.gallery.feature_node.data.data_types.findMedia
import com.dot.gallery.feature_node.data.data_types.getAlbums
import com.dot.gallery.feature_node.data.data_types.getMedia
import com.dot.gallery.feature_node.data.data_types.getMediaByUri
import com.dot.gallery.feature_node.data.data_types.getMediaFavorite
import com.dot.gallery.feature_node.data.data_types.getMediaTrashed
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class MediaRepositoryImpl(
    private val context: Context
) : MediaRepository {
    private val contentResolver by lazy { context.contentResolver }

    override suspend fun getMedia(): Flow<Resource<List<Media>>> = flow {
        try {
            emit(Resource.Loading())
            val media = contentResolver.getMedia(mediaOrder = MediaOrder.Date(OrderType.Descending))
            emit(Resource.Success(data = media))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "An error occurred"))
        }
    }

    override suspend fun getFavorites(mediaOrder: MediaOrder): Flow<Resource<List<Media>>> = flow {
        try {
            emit(Resource.Loading())
            val media = contentResolver.getMediaFavorite(mediaOrder = mediaOrder)
            emit(Resource.Success(data = media))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "An error occurred"))
        }
    }

    override suspend fun getTrashed(mediaOrder: MediaOrder): Flow<Resource<List<Media>>> = flow {
        try {
            emit(Resource.Loading())
            val media = contentResolver.getMediaTrashed(mediaOrder = mediaOrder)
            emit(Resource.Success(data = media))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "An error occurred"))
        }
    }

    override suspend fun getAlbums(mediaOrder: MediaOrder): Flow<Resource<List<Album>>> = flow {
        try {
            emit(Resource.Loading())
            val media =
                contentResolver.getAlbums(mediaOrder = mediaOrder)
            emit(Resource.Success(data = media))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "An error occurred"))
        }
    }

    override suspend fun insertMedia(
        media: Media,
        callback: MediaScannerConnection.OnScanCompletedListener
    ) {
        MediaScannerConnection.scanFile(context, arrayOf(media.path), null, callback)
    }

    @SuppressLint("Range")
    override suspend fun getMediaById(mediaId: Long): Media? {
        val query = Query.MediaQuery().copy(
            bundle = Bundle().apply {
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    MediaStore.MediaColumns._ID + "= ?"
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    arrayOf(mediaId.toString())
                )
            }
        )
        return contentResolver.findMedia(query)
    }

    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>> = flow {
        try {
            emit(Resource.Loading())
            val query = Query.MediaQuery().copy(
                bundle = Bundle().apply {
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        MediaStore.MediaColumns.BUCKET_ID + "= ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(albumId.toString())
                    )
                }
            )
            val media = contentResolver.getMedia(query)
            emit(Resource.Success(data = media))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "An error occurred"))
        }
    }

    override fun getMediaByUri(uriAsString: String): Flow<Resource<List<Media>>> = flow {
        try {
            emit(Resource.Loading())
            val media = contentResolver.getMediaByUri(Uri.parse(uriAsString))
            if (media == null) {
                emit(Resource.Error(message = "Media could not be opened"))
            } else {
                emit(Resource.Success(data = listOf(media)))
            }
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "An error occurred"))
        }
    }
}