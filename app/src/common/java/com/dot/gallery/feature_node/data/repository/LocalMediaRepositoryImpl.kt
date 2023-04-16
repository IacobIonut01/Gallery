package com.dot.gallery.feature_node.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.dot.gallery.core.contentFlowObserver
import com.dot.gallery.feature_node.data.data_source.AlbumDao
import com.dot.gallery.feature_node.data.data_source.MediaDao
import com.dot.gallery.feature_node.data.data_types.getMediaByUri
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class LocalMediaRepositoryImpl(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val albumDao: AlbumDao
) : MediaRepository {

    private val contentResolver by lazy { context.contentResolver }

    override fun getMedia(): Flow<List<Media>> =
        mediaDao.getMedia().map { MediaOrder.Date(OrderType.Descending).sortMedia(it) }

    override fun getFavorites(mediaOrder: MediaOrder): Flow<List<Media>> =
        mediaDao.getFavorites().map { mediaOrder.sortMedia(it) }

    override fun getTrashed(mediaOrder: MediaOrder): Flow<List<Media>> =
        mediaDao.getTrashed().map { mediaOrder.sortMedia(it) }

    override fun getAlbums(mediaOrder: MediaOrder): Flow<List<Album>> =
        albumDao.getAlbums().map { mediaOrder.sortAlbums(it) }

    override suspend fun insertMedia(media: Media) =
        mediaDao.insertMedia(media)

    override suspend fun getMediaById(mediaId: Long): Media? =
        mediaDao.getMediaById(mediaId)

    override fun getMediaByAlbumId(albumId: Long): Flow<List<Media>> =
        mediaDao.getMediaByAlbumId(albumId)

    override fun getMediaByUri(uriAsString: String): Flow<List<Media>> =
        contentResolver.retrieveMediaAsResource {
            val item = it.getMediaByUri(Uri.parse(uriAsString))
            item?.let { media -> listOf(media) } ?: emptyList()
        }

    override suspend fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean
    ) {
        mediaDao.insertMedia(mediaList.map { it.copy(favorite = if (favorite) 1 else 0) })
    }

    override suspend fun trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        trash: Boolean
    ) {
        mediaDao.insertMedia(mediaList.map { it.copy(trashed = if (trash) 1 else 0) })
    }

    override suspend fun deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>
    ) {
        mediaDao.removeMedia(mediaList)
    }

    companion object {
        val URIs = arrayOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        fun ContentResolver.retrieveMediaAsResource(dataBody: suspend (ContentResolver) -> List<Media>) =
            contentFlowObserver(URIs).map {
                dataBody.invoke(this)
            }
    }
}