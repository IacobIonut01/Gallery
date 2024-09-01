package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaVersion
import com.dot.gallery.feature_node.domain.model.TimelineSettings
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    /** Media */
    @Query("SELECT * FROM media ORDER BY timestamp DESC")
    suspend fun getMedia(): List<Media>

    @Query("SELECT * FROM media WHERE mimeType LIKE :allowedMedia ORDER BY timestamp DESC")
    suspend fun getMediaByType(allowedMedia: AllowedMedia): List<Media>

    @Query("SELECT * FROM media WHERE favorite = 1 ORDER BY timestamp DESC")
    suspend fun getFavorites(): List<Media>

    @Query("SELECT * FROM media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): Media

    @Query("SELECT * FROM media WHERE albumID = :albumId ORDER BY timestamp DESC")
    suspend fun getMediaByAlbumId(albumId: Long): List<Media>

    @Query("SELECT * FROM media WHERE albumID = :albumId AND mimeType LIKE :allowedMedia ORDER BY timestamp DESC")
    suspend fun getMediaByAlbumIdAndType(albumId: Long, allowedMedia: AllowedMedia): List<Media>

    @Upsert(entity = Media::class)
    suspend fun addMediaList(mediaList: List<Media>)

    @Transaction
    suspend fun updateMedia(mediaList: List<Media>) {
        // Upsert the items in mediaList
        addMediaList(mediaList)

        // Get the IDs of the media items in mediaList
        val mediaIds = mediaList.map { it.id }

        // Delete items from the database that are not in mediaList
        deleteMediaNotInList(mediaIds)
    }

    @Query("DELETE FROM media WHERE id NOT IN (:mediaIds)")
    suspend fun deleteMediaNotInList(mediaIds: List<Long>)

    /** MediaVersion */
    @Upsert(entity = MediaVersion::class)
    suspend fun setMediaVersion(version: MediaVersion)

    @Query("SELECT EXISTS(SELECT * FROM media_version WHERE version = :version) LIMIT 1")
    suspend fun isMediaVersionUpToDate(version: String): Boolean

    /** Timeline Settings */
    @Query("SELECT * FROM timeline_settings LIMIT 1")
    fun getTimelineSettings(): Flow<TimelineSettings?>

    @Upsert(entity = TimelineSettings::class)
    suspend fun setTimelineSettings(settings: TimelineSettings)

}