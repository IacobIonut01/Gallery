package com.dot.gallery.feature_node.data.data_source

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever.*
import android.media.MediaScannerConnection
import android.provider.MediaStore
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.data.data_types.findMedia
import com.dot.gallery.feature_node.data.data_types.getAlbums
import com.dot.gallery.feature_node.data.data_types.getMedia
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MediaParserImpl @Inject constructor(
    private val context: Context
) : MediaParser {

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

    override suspend fun getAlbum(): Flow<Resource<List<Album>>> = flow {
        try {
            emit(Resource.Loading())
            val media = contentResolver.getAlbums(mediaOrder = MediaOrder.Date(OrderType.Descending))
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
        val imageObject = MediaQuery.PhotoQuery().copy(
            selection = "${MediaStore.Images.Media._ID} = ?",
            selectionArgs = arrayOf(mediaId.toString())
        )
        val videoObject = MediaQuery.VideoQuery().copy(
            selection = "${MediaStore.Video.Media._ID} = ?",
            selectionArgs = arrayOf(mediaId.toString())
        )
        return contentResolver.findMedia(arrayListOf(imageObject, videoObject))
    }

    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>> = flow {
        try {
            emit(Resource.Loading())
            val queries = arrayListOf(
                MediaQuery.PhotoQuery().copy(
                    selection = "${MediaStore.Images.Media.BUCKET_ID} = ?",
                    selectionArgs = arrayOf(albumId.toString())
                ),
                MediaQuery.VideoQuery().copy(
                    selection = "${MediaStore.Video.Media.BUCKET_ID} = ?",
                    selectionArgs = arrayOf(albumId.toString())
                )
            )
            val media = contentResolver.getMedia(queries)
            emit(Resource.Success(data = media))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "An error occurred"))
        }
    }

    override suspend fun deleteMedia(mediaId: Long) {

    }

    override suspend fun deleteMedia(media: Media) {

    }

}