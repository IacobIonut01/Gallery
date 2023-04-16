package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.Album
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM album_table ORDER BY timestamp DESC")
    fun getAlbums(): Flow<List<Album>>

    @Upsert
    fun insertAlbum(album: Album)

    @Upsert
    fun insertAlbum(list: List<Album>)

    @Delete
    fun removeAlbum(album: Album)

    @Delete
    fun removeAlbum(list: List<Album>)

}