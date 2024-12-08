package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {

    @Query("SELECT * FROM blacklist")
    fun getBlacklistedAlbums(): Flow<List<IgnoredAlbum>>

    @Query("SELECT * FROM blacklist")
    suspend fun getBlacklistedAlbumsAsync(): List<IgnoredAlbum>

    @Upsert
    suspend fun addBlacklistedAlbum(ignoredAlbum: IgnoredAlbum)

    @Delete
    suspend fun removeBlacklistedAlbum(ignoredAlbum: IgnoredAlbum)

}