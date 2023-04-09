package com.dot.gallery.feature_node.data.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.core.Resource
import com.dot.gallery.core.contentFlowObserver
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
import kotlinx.coroutines.flow.map

abstract class MediaRepositoryImpl(
    private val context: Context
) : MediaRepository {
    private val contentResolver by lazy { context.contentResolver }

    /**
     * TODO: Add media reordering
     */
    override fun getMedia(): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMedia { it.getMedia(mediaOrder = DEFAULT_ORDER) }

    override fun getFavorites(mediaOrder: MediaOrder): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMedia { it.getMediaFavorite(mediaOrder = mediaOrder) }

    override fun getTrashed(mediaOrder: MediaOrder): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMedia { it.getMediaTrashed(mediaOrder = mediaOrder) }

    override fun getAlbums(mediaOrder: MediaOrder): Flow<Resource<List<Album>>> =
        contentResolver.retrieveAlbums { it.getAlbums(mediaOrder = mediaOrder) }

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

    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMedia {
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
            /** return@retrieveMedia */
            it.getMedia(query)
        }

    override fun getMediaByUri(uriAsString: String): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMediaAsResource {
            val media = it.getMediaByUri(Uri.parse(uriAsString))
            /** return@retrieveMediaAsResource */
            if (media == null) {
                Resource.Error(message = "Media could not be opened")
            } else {
                Resource.Success(data = listOf(media))
            }
        }

    companion object {
        private val DEFAULT_ORDER = MediaOrder.Date(OrderType.Descending)
        private val URIs = arrayOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        private fun ContentResolver.retrieveMediaAsResource(dataBody: suspend (ContentResolver) -> Resource<List<Media>>) =
            contentFlowObserver(URIs).map {
                try {
                    dataBody.invoke(this)
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }

        private fun ContentResolver.retrieveMedia(dataBody: suspend (ContentResolver) -> List<Media>) =
            contentFlowObserver(URIs).map {
                try {
                    Resource.Success(data = dataBody.invoke(this))
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }
        private fun ContentResolver.retrieveAlbums(dataBody: suspend (ContentResolver) -> List<Album>) =
            contentFlowObserver(URIs).map {
                try {
                    Resource.Success(data = dataBody.invoke(this))
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }
    }
}