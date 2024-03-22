package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.CustomAlbum
import com.dot.gallery.feature_node.domain.model.CustomAlbumItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAlbumDao {

    // this query returns all custom albums, and counts their mediaitems.
    @Query("SELECT id, label, timestamp, isPinned, tmptable.count as count FROM customalbum " +
            "LEFT JOIN " +
            "(SELECT customalbum_items.albumId, COUNT(*) as count FROM customalbum_items GROUP BY customalbum_items.albumId) as tmptable " +
            "ON customalbum.id = tmptable.albumId")
    fun getCustomAlbums(): Flow<List<CustomAlbum>>

    @Upsert
    suspend fun addCustomAlbum(album: CustomAlbum): Long

    @Delete
    suspend fun deleteCustomAlbum(album: CustomAlbum)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addCustomAlbumItem(item: CustomAlbumItem)

    @Query("SELECT * FROM customalbum_items")
    fun getCustomAlbumItems(): Flow<List<CustomAlbumItem>>


    @MapInfo(keyColumn = "albumId", valueColumn = "id")
    @Query("SELECT * FROM customalbum_items WHERE albumId = :customAlbumId")
    fun getCustomAlbumItemsForAlbum(customAlbumId: Long): List<CustomAlbumItem>




}