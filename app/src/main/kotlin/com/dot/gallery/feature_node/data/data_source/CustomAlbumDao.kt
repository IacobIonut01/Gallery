package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.CustomAlbum
import com.dot.gallery.feature_node.domain.model.CustomAlbumItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAlbumDao {

    @Query("SELECT * FROM customalbum")
    fun getCustomAlbums(): Flow<List<CustomAlbum>>

    @Upsert
    suspend fun addCustomAlbum(album: CustomAlbum)

    @Delete
    suspend fun deleteCustomAlbum(album: CustomAlbum)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addCustomAlbumItem(item: CustomAlbumItem)

    @Query("SELECT * FROM customalbum_items")
    fun getCustomAlbumItems(): Flow<List<CustomAlbumItem>>


    @Query("SELECT * FROM customalbum_items WHERE albumId = :customAlbumId")
    fun getCustomAlbumItemsForAlbum(customAlbumId: Long): Flow<List<CustomAlbumItem>>




}