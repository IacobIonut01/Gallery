package com.dot.gallery.feature_node.data.data_source

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class MediaDaoImpl @Inject constructor(
    private val context: Context
) : MediaDao {

    override fun getMedia(): Flow<Resource<List<Media>>> = flow {
        try {
            emit(Resource.Loading())
            val media = getMedia(mediaOrder = MediaOrder.Date(OrderType.Descending))
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
        var media: Media? = null
        val imageObject = imageObject.copy(
            selection = "${MediaStore.Images.Media._ID} = ?",
            selectionArgs = arrayOf(mediaId.toString())
        )
        getCursor(imageObject) {
            media = getMediaFromCursor(it, imageObject.uri)
            it.close()
        }
        if (media == null) {
            val videoObject = videoObject.copy(
                selection = "${MediaStore.Video.Media._ID} = ?",
                selectionArgs = arrayOf(mediaId.toString())
            )
            getCursor(videoObject) {
                media = getMediaFromCursor(it, videoObject.uri)
                it.close()
            }
        }
        return media
    }


    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>> = flow {
        try {
            emit(Resource.Loading())
            val mediaImages = getMedia(
                mediaObject = imageObject.copy(
                    selection = "${MediaStore.Images.Media.BUCKET_ID} = ?",
                    selectionArgs = arrayOf(albumId.toString())
                )
            )
            val mediaVideos = getMedia(
                mediaObject = videoObject.copy(
                    selection = "${MediaStore.Video.Media.BUCKET_ID} = ?",
                    selectionArgs = arrayOf(albumId.toString())
                )
            )
            val media = ArrayList<Media>().apply {
                addAll(mediaImages)
                addAll(mediaVideos)
                sortedByDescending { it.timestamp }
            }
            emit(Resource.Success(data = media))
        } catch (e: Exception) {
            emit(Resource.Error(message = e.localizedMessage ?: "An error occurred"))
        }
    }

    override suspend fun deleteMedia(mediaId: Long) {

    }

    override suspend fun deleteMedia(media: Media) {

    }

    @SuppressLint("Range")
    private fun getMediaFromCursor(cursor: Cursor, uri: Uri): Media? {
        with(cursor) {
            val isVideo = uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val id: Long
            val path: String?
            val title: String?
            val albumID: Long
            val timestamp: Long
            val duration: String?
            if (isVideo) {
                id = getLong(getColumnIndex(MediaStore.Video.Media._ID))
                path = getString(getColumnIndex(MediaStore.Video.Media.DATA))
                title = getString(getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME))
                albumID = getLong(getColumnIndex(MediaStore.Video.Media.BUCKET_ID))
                timestamp =
                    getLong(getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED))
                duration = getString(getColumnIndex(MediaStore.Video.Media.DURATION))
            } else {
                id = getLong(getColumnIndex(MediaStore.Images.Media._ID))
                path = getString(getColumnIndex(MediaStore.Images.Media.DATA))
                title = getString(getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
                albumID = getLong(getColumnIndex(MediaStore.Images.Media.BUCKET_ID))
                timestamp =
                    getLong(getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED))
                duration = null
            }
            if (path != null && title != null) {
                return Media(
                    id = id,
                    label = title,
                    uri = Uri.fromFile(File(path)),
                    path = path,
                    albumID = albumID,
                    timestamp = timestamp,
                    duration = duration
                )
            }
        }
        return null
    }

    private fun getCursor(
        mediaObject: MediaObject,
        callback: (Cursor) -> Unit
    ) {
        context.contentResolver.query(
            mediaObject.uri,
            mediaObject.projection,
            mediaObject.selection,
            mediaObject.selectionArgs,
            mediaObject.sortOrder
        )?.let {
            while (it.moveToNext()) {
                try {
                    callback.invoke(it)
                } catch (e: Exception) {
                    Log.d("GalleryException", e.message.toString())
                }
            }
            it.close()
        }
    }

    private fun getDeleteUri(media: Media): Uri? {
        val mediaUri =
            if (media.duration != null)
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val mediaProjection =
            if (media.duration != null)
                MediaStore.Video.Media._ID
            else
                MediaStore.Images.Media._ID

        val mediaSelection =
            if (media.duration != null)
                MediaStore.Video.Media.DATA + " = ?"
            else
                MediaStore.Images.Media.DATA + " = ?"

        val cursor = context.contentResolver.query(
            mediaUri,
            arrayOf(mediaProjection),
            mediaSelection,
            arrayOf(media.path),
            null
        )
        val uri = if (cursor != null && cursor.moveToFirst())
            ContentUris.withAppendedId(
                mediaUri,
                cursor.getLong(cursor.getColumnIndexOrThrow(mediaProjection))
            ) else null
        cursor?.close()
        return uri
    }

    private fun getMedia(
        mediaObject: MediaObject,
        mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
    ): List<Media> {
        val media = ArrayList<Media>()
        val sortBy = when (mediaOrder) {
            is MediaOrder.Date -> MediaStore.Video.Media.DATE_MODIFIED
            is MediaOrder.Label -> MediaStore.Video.Media.TITLE
        }
        val sortType = when (mediaOrder.orderType) {
            OrderType.Ascending -> "ASC"
            OrderType.Descending -> "DESC"
        }
        getCursor(
            mediaObject.copy(
                sortOrder = "$sortBy $sortType"
            )
        ) { cursor ->
            getMediaFromCursor(cursor, mediaObject.uri)?.let { media.add(it) }
        }
        return media
    }

    private fun getMedia(mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)): List<Media> {
        val media = ArrayList<Media>()
        media.addAll(
            getImages(mediaOrder)
        )
        media.addAll(
            getVideos(mediaOrder)
        )

        when (mediaOrder.orderType) {
            OrderType.Ascending -> {
                when (mediaOrder) {
                    is MediaOrder.Date -> media.sortedBy { it.timestamp }
                    is MediaOrder.Label -> media.sortedBy { it.label.lowercase() }
                }
            }

            OrderType.Descending -> {
                when (mediaOrder) {
                    is MediaOrder.Date -> media.sortedByDescending { it.timestamp }
                    is MediaOrder.Label -> media.sortedByDescending { it.label.lowercase() }
                }
            }
        }
        return media
    }

    @SuppressLint("Range")
    private fun getImages(mediaOrder: MediaOrder): List<Media> {
        val media = ArrayList<Media>()
        val sortBy = when (mediaOrder) {
            is MediaOrder.Date -> MediaStore.Images.Media.DATE_MODIFIED
            is MediaOrder.Label -> MediaStore.Images.Media.TITLE
        }
        val sortType = when (mediaOrder.orderType) {
            OrderType.Ascending -> "ASC"
            OrderType.Descending -> "DESC"
        }
        getCursor(
            imageObject.copy(
                sortOrder = "$sortBy $sortType"
            )
        ) { cursor ->
            getMediaFromCursor(cursor, imageObject.uri)?.let { media.add(it) }
        }
        return media
    }

    @SuppressLint("Range")
    private fun getVideos(mediaOrder: MediaOrder): List<Media> {
        val media = ArrayList<Media>()
        val sortBy = when (mediaOrder) {
            is MediaOrder.Date -> MediaStore.Video.Media.DATE_MODIFIED
            is MediaOrder.Label -> MediaStore.Video.Media.TITLE
        }
        val sortType = when (mediaOrder.orderType) {
            OrderType.Ascending -> "ASC"
            OrderType.Descending -> "DESC"
        }
        getCursor(
            videoObject.copy(
                sortOrder = "$sortBy $sortType"
            )
        ) { cursor ->
            getMediaFromCursor(cursor, videoObject.uri)?.let { media.add(it) }
        }
        return media
    }

    companion object {
        val imageObject = MediaObject(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.DATE_MODIFIED
            )
        )
        val videoObject = MediaObject(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DURATION
            )
        )
    }

}