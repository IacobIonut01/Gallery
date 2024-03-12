package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.CustomAlbum
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAlbumDao {

    @Query("SELECT * FROM customalbum")
    fun getCustomAlbums(): Flow<List<CustomAlbum>>

    @Upsert
    suspend fun addBlacklistedAlbum(album: CustomAlbum)

    @Delete
    suspend fun removeBlacklistedAlbum(album: CustomAlbum)

}