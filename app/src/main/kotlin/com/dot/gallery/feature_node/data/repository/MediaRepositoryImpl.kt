/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityOptionsCompat
import com.dot.gallery.core.Resource
import com.dot.gallery.core.contentFlowObserver
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.Query
import com.dot.gallery.feature_node.data.data_types.findMedia
import com.dot.gallery.feature_node.data.data_types.getAlbums
import com.dot.gallery.feature_node.data.data_types.getMedia
import com.dot.gallery.feature_node.data.data_types.getMediaByUri
import com.dot.gallery.feature_node.data.data_types.getMediaFavorite
import com.dot.gallery.feature_node.data.data_types.getMediaListByUris
import com.dot.gallery.feature_node.data.data_types.getMediaTrashed
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.PinnedAlbum
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaRepositoryImpl(
    private val contentResolver: ContentResolver,
    private val database: InternalDatabase
) : MediaRepository {

    /**
     * TODO: Add media reordering
     */
    override fun getMedia(): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMedia { it.getMedia(mediaOrder = DEFAULT_ORDER) }

    override fun getMediaByType(allowedMedia: AllowedMedia): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMedia {
            val query = when (allowedMedia) {
                PHOTOS -> Query.PhotoQuery()
                VIDEOS -> Query.VideoQuery()
                BOTH -> Query.MediaQuery()
            }
            it.getMedia(mediaQuery = query, mediaOrder = DEFAULT_ORDER)
        }

    override fun getFavorites(mediaOrder: MediaOrder): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMedia { it.getMediaFavorite(mediaOrder = mediaOrder) }

    override fun getTrashed(mediaOrder: MediaOrder): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMedia { it.getMediaTrashed(mediaOrder = mediaOrder) }

    override fun getAlbums(mediaOrder: MediaOrder): Flow<Resource<List<Album>>> =
        contentResolver.retrieveAlbums {
            it.getAlbums(mediaOrder = mediaOrder).toMutableList().apply {
                replaceAll { album ->
                    album.copy(isPinned = database.getPinnedDao().albumIsPinned(album.id))
                }
            }
        }

    override suspend fun insertPinnedAlbum(pinnedAlbum: PinnedAlbum) =
        database.getPinnedDao().insertPinnedAlbum(pinnedAlbum)

    override suspend fun removePinnedAlbum(pinnedAlbum: PinnedAlbum) =
        database.getPinnedDao().removePinnedAlbum(pinnedAlbum)

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

    override fun getMediaByAlbumIdWithType(
        albumId: Long,
        allowedMedia: AllowedMedia
    ): Flow<Resource<List<Media>>>  =
        contentResolver.retrieveMedia {
            val query = Query.MediaQuery().copy(
                bundle = Bundle().apply {
                    val mimeType = when (allowedMedia) {
                        PHOTOS -> "image%"
                        VIDEOS -> "video%"
                        BOTH -> "%/%"
                    }
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        MediaStore.MediaColumns.BUCKET_ID + "= ? and " + MediaStore.MediaColumns.MIME_TYPE + " like ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(albumId.toString(), mimeType)
                    )
                }
            )
            /** return@retrieveMedia */
            it.getMedia(query)
        }

    override fun getAlbumsWithType(allowedMedia: AllowedMedia): Flow<Resource<List<Album>>> =
        contentResolver.retrieveAlbums {
            val query = Query.AlbumQuery().copy(
                bundle = Bundle().apply {
                    val mimeType = when (allowedMedia) {
                        PHOTOS -> "image%"
                        VIDEOS -> "video%"
                        BOTH -> "%/%"
                    }
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        MediaStore.MediaColumns.MIME_TYPE + " like ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(mimeType)
                    )
                }
            )
            it.getAlbums(query, mediaOrder = MediaOrder.Label(OrderType.Ascending))
        }

    override fun getMediaByUri(
        uriAsString: String,
        isSecure: Boolean
    ): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMediaAsResource {
            val media = it.getMediaByUri(Uri.parse(uriAsString))
            /** return@retrieveMediaAsResource */
            if (media == null) {
                Resource.Error(message = "Media could not be opened")
            } else {
                val query = Query.MediaQuery().copy(
                    bundle = Bundle().apply {
                        putString(
                            ContentResolver.QUERY_ARG_SQL_SELECTION,
                            MediaStore.MediaColumns.BUCKET_ID + "= ?"
                        )
                        putStringArray(
                            ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                            arrayOf(media.albumID.toString())
                        )
                    }
                )
                Resource.Success(
                    data = if (isSecure) listOf(media) else it.getMedia(query)
                        .ifEmpty { listOf(media) })
            }
        }

    override fun getMediaListByUris(listOfUris: List<Uri>): Flow<Resource<List<Media>>> =
        contentResolver.retrieveMediaAsResource {
            val mediaList = it.getMediaListByUris(listOfUris)
            if (mediaList.isEmpty()) {
                Resource.Error(message = "Media could not be opened")
            } else {
                Resource.Success(data = mediaList)
            }
        }

    override suspend fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean
    ) {
        val intentSender = MediaStore.createFavoriteRequest(
            contentResolver,
            mediaList.map { it.uri },
            favorite
        ).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest)
    }

    override suspend fun trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        trash: Boolean
    ) {
        val intentSender = MediaStore.createTrashRequest(
            contentResolver,
            mediaList.map { it.uri },
            trash
        ).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest, ActivityOptionsCompat.makeTaskLaunchBehind())
    }

    override suspend fun deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>
    ) {
        val intentSender =
            MediaStore.createDeleteRequest(contentResolver, mediaList.map { it.uri }).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest)
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