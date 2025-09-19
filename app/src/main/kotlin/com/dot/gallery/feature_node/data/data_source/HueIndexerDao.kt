package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.flow.Flow

@Dao
interface HueIndexerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIndexedMedia(entry: Media.HueIndexedMedia)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMultipleIndexedMedia(entries: List<Media.HueIndexedMedia>)

    @Query("SELECT * FROM hue_indexed_media")
    suspend fun getIndexedMedia(): List<Media.HueIndexedMedia>

    @Query("SELECT * FROM hue_indexed_media")
    fun getIndexedMediaFlow(): Flow<List<Media.HueIndexedMedia>>

    @Query("SELECT COUNT(*) FROM hue_indexed_media")
    fun getIndexedMediaCountFlow(): Flow<Int>

    @Query("SELECT * FROM hue_indexed_media WHERE morton1 IN (:codes) OR morton2 IN (:codes) ORDER BY timestamp DESC")
    fun getMatchesByNeighborhoodFlow(codes: List<Long>): Flow<List<Media.HueIndexedMedia>>

    @Query("DELETE FROM hue_indexed_media WHERE id NOT IN (:mediaIds)")
    suspend fun deleteDeindexedMedia(mediaIds: List<Long>)

    @Query("DELETE FROM hue_indexed_media")
    suspend fun deleteAllIndexedMedia()
}