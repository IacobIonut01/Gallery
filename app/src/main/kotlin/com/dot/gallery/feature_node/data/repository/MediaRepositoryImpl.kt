/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import com.dot.gallery.feature_node.data.data_types.copyMedia
import com.dot.gallery.feature_node.data.data_types.findMedia
import com.dot.gallery.feature_node.data.data_types.getAlbums
import com.dot.gallery.feature_node.data.data_types.getMedia
import com.dot.gallery.feature_node.data.data_types.getMediaByUri
import com.dot.gallery.feature_node.data.data_types.getMediaFavorite
import com.dot.gallery.feature_node.data.data_types.getMediaListByUris
import com.dot.gallery.feature_node.data.data_types.overrideImage
import com.dot.gallery.feature_node.data.data_types.saveImage
import com.dot.gallery.feature_node.data.data_types.updateMedia
import com.dot.gallery.feature_node.data.data_types.updateMediaExif
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.BlacklistedAlbum
import com.dot.gallery.feature_node.domain.model.ExifAttributes
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.PinnedAlbum
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.BOTH
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.PHOTOS
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.VIDEOS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

class MediaRepositoryImpl(
    private val context: Context,
    private val database: InternalDatabase
) : MediaRepository {

    /**
     * TODO: Add media reordering
     */
    override fun getMedia(): Flow<Resource<List<Media>>> =
        context.retrieveMedia {
            it.getMedia(mediaOrder = DEFAULT_ORDER).removeBlacklisted()
        }

    override fun getMediaByType(allowedMedia: AllowedMedia): Flow<Resource<List<Media>>> =
        context.retrieveMedia {
            val query = when (allowedMedia) {
                PHOTOS -> Query.PhotoQuery()
                VIDEOS -> Query.VideoQuery()
                BOTH -> Query.MediaQuery()
            }
            it.getMedia(mediaQuery = query, mediaOrder = DEFAULT_ORDER).removeBlacklisted()
        }

    override fun getFavorites(mediaOrder: MediaOrder): Flow<Resource<List<Media>>> =
        context.retrieveMedia {
            it.getMediaFavorite(mediaOrder = mediaOrder).removeBlacklisted()
        }

    override fun getTrashed(): Flow<Resource<List<Media>>> =
        context.retrieveMedia {
            it.getMediaTrashed().removeBlacklisted()
        }

    override fun getAlbums(mediaOrder: MediaOrder, ignoreBlacklisted: Boolean): Flow<Resource<List<Album>>> =
        context.retrieveAlbums {
            it.getAlbums(mediaOrder = mediaOrder).toMutableList().apply {
                replaceAll { album ->
                    album.copy(isPinned = database.getPinnedDao().albumIsPinned(album.id))
                }
                if (!ignoreBlacklisted) {
                    removeAll { album ->
                        database.getBlacklistDao().albumIsBlacklisted(album.id)
                    }
                }
            }
        }

    override suspend fun insertPinnedAlbum(pinnedAlbum: PinnedAlbum) =
        database.getPinnedDao().insertPinnedAlbum(pinnedAlbum)

    override suspend fun removePinnedAlbum(pinnedAlbum: PinnedAlbum) =
        database.getPinnedDao().removePinnedAlbum(pinnedAlbum)

    override suspend fun addBlacklistedAlbum(blacklistedAlbum: BlacklistedAlbum) =
        database.getBlacklistDao().addBlacklistedAlbum(blacklistedAlbum)

    override suspend fun removeBlacklistedAlbum(blacklistedAlbum: BlacklistedAlbum) =
        database.getBlacklistDao().removeBlacklistedAlbum(blacklistedAlbum)

    override fun getBlacklistedAlbums(): Flow<List<BlacklistedAlbum>> =
        database.getBlacklistDao().getBlacklistedAlbums()

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
        return context.contentResolver.findMedia(query)
    }

    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>> =
        context.retrieveMedia {
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
    ): Flow<Resource<List<Media>>> =
        context.retrieveMedia {
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
        context.retrieveAlbums {
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
        context.retrieveMediaAsResource {
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

    override fun getMediaListByUris(
        listOfUris: List<Uri>,
        reviewMode: Boolean
    ): Flow<Resource<List<Media>>> =
        context.retrieveMediaAsResource {
            var mediaList = it.getMediaListByUris(listOfUris)
            if (reviewMode) {
                val query = Query.MediaQuery().copy(
                    bundle = Bundle().apply {
                        putString(
                            ContentResolver.QUERY_ARG_SQL_SELECTION,
                            MediaStore.MediaColumns.BUCKET_ID + "= ?"
                        )
                        putStringArray(
                            ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                            arrayOf(mediaList.first().albumID.toString())
                        )
                    }
                )
                mediaList = it.getMedia(query)
            }
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
            context.contentResolver,
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
            context.contentResolver,
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
            MediaStore.createDeleteRequest(
                context.contentResolver,
                mediaList.map { it.uri }).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest)
    }

    override suspend fun copyMedia(
        from: Media,
        path: String
    ): Boolean = context.contentResolver.copyMedia(
        from = from,
        path = path
    )

    override suspend fun renameMedia(
        media: Media,
        newName: String
    ): Boolean = context.contentResolver.updateMedia(
        media = media,
        contentValues = displayName(newName)
    )

    override suspend fun moveMedia(
        media: Media,
        newPath: String
    ): Boolean = context.contentResolver.updateMedia(
        media = media,
        contentValues = relativePath(newPath)
    )

    override suspend fun updateMediaExif(
        media: Media,
        exifAttributes: ExifAttributes
    ): Boolean = context.contentResolver.updateMediaExif(
        media = media,
        exifAttributes = exifAttributes
    )

    override fun saveImage(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = context.contentResolver.saveImage(bitmap, format, mimeType, relativePath, displayName)

    override fun overrideImage(
        uri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = context.contentResolver.overrideImage(uri, bitmap, format, mimeType, relativePath, displayName)

    companion object {
        private val DEFAULT_ORDER = MediaOrder.Date(OrderType.Descending)
        private val URIs = arrayOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        private fun displayName(newName: String) = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
        }

        private fun relativePath(newPath: String) = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, newPath)
        }

        private fun Context.retrieveMediaAsResource(dataBody: suspend (ContentResolver) -> Resource<List<Media>>) =
            contentFlowObserver(URIs).map {
                try {
                    dataBody.invoke(contentResolver)
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }.conflate()

        private fun Context.retrieveMedia(dataBody: suspend (ContentResolver) -> List<Media>) =
            contentFlowObserver(URIs).map {
                try {
                    Resource.Success(data = dataBody.invoke(contentResolver))
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }.conflate()

        private fun Context.retrieveAlbums(dataBody: suspend (ContentResolver) -> List<Album>) =
            contentFlowObserver(URIs).map {
                try {
                    Resource.Success(data = dataBody.invoke(contentResolver))
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }.conflate()
    }
}