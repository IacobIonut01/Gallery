package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.flow.Flow

@Dao
interface HueClassifierDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClassifiedMedia(entry: Media.HueClassifiedMedia)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMultipleClassifiedMedia(entries: List<Media.HueClassifiedMedia>)

    @Query("SELECT * FROM hue_classified_media")
    suspend fun getClassifiedMedia(): List<Media.HueClassifiedMedia>

    @Query("SELECT * FROM hue_classified_media")
    fun getClassifiedMediaFlow(): Flow<List<Media.HueClassifiedMedia>>

    @Query("SELECT COUNT(*) FROM hue_classified_media")
    fun getClassifiedMediaCountFlow(): Flow<Int>

    @Query("SELECT * FROM hue_classified_media WHERE morton1 IN (:codes) OR morton2 IN (:codes) ORDER BY timestamp DESC")
    fun getMatchesByNeighborhoodFlow(codes: List<Long>): Flow<List<Media.HueClassifiedMedia>>

    @Query("DELETE FROM hue_classified_media WHERE id NOT IN (:mediaIds)")
    suspend fun deleteDeclassifiedMedia(mediaIds: List<Long>)

    @Query("DELETE FROM hue_classified_media")
    suspend fun deleteAllClassifiedMedia()
}