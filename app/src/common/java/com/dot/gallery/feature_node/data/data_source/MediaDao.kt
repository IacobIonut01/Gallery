package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query(DEFAULT_MEDIA_QUERY)
    fun getMedia(): Flow<List<Media>>

    @Query("$DEFAULT_QUERY WHERE id = :id")
    suspend fun getMediaById(id: Long): Media?

    @Query("$DEFAULT_MEDIA_QUERY AND albumID = :albumId $DEFAULT_SORT")
    fun getMediaByAlbumId(albumId: Long): Flow<List<Media>>

    @Query("$DEFAULT_MEDIA_QUERY AND favorite = 1 $DEFAULT_SORT")
    fun getFavorites(): Flow<List<Media>>

    @Query("$DEFAULT_QUERY WHERE trashed = 1 $DEFAULT_SORT")
    fun getTrashed(): Flow<List<Media>>

    @Upsert
    suspend fun insertMedia(media: Media)

    @Upsert
    suspend fun insertMedia(list: List<Media>)

    @Delete
    suspend fun removeMedia(media: Media)

    @Delete
    suspend fun removeMedia(list: List<Media>)

    companion object {
        private const val DEFAULT_QUERY = "SELECT * FROM media_table"
        private const val DEFAULT_SORT = "ORDER BY timestamp DESC"
        private const val DEFAULT_MEDIA_QUERY = "$DEFAULT_QUERY WHERE trashed = 0"
    }

}