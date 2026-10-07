package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.dot.gallery.domain.model.AlbumThumbnail
import kotlinx.coroutines.flow.Flow



@Dao
interface AlbumThumbnailDao {

    @Upsert
    suspend fun updateAlbumThumbnail(albumThumbnail: AlbumThumbnail)

    @Query("DELETE FROM album_thumbnail WHERE albumId = :albumId")
    suspend fun deleteAlbumThumbnail(albumId: Long)

    @Query("SELECT * FROM album_thumbnail WHERE albumId = :albumId")
    fun getAlbumThumbnail(albumId: Long): Flow<AlbumThumbnail?>
    @Query("SELECT EXISTS(SELECT * FROM album_thumbnail WHERE albumId = :albumId) LIMIT 1")
    fun hasAlbumThumbnail(albumId: Long): Flow<Boolean>

    @Query("SELECT * FROM album_thumbnail")
    suspend fun getAlbumThumbnails(): List<AlbumThumbnail>

    @Query("SELECT * FROM album_thumbnail")
    fun getAlbumThumbnailsFlow(): Flow<List<AlbumThumbnail>>

}