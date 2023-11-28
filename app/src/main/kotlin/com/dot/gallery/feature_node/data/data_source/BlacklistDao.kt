package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.BlacklistedAlbum
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {

    @Query("SELECT * FROM blacklist")
    fun getBlacklistedAlbums(): Flow<List<BlacklistedAlbum>>

    @Upsert
    suspend fun addBlacklistedAlbum(blacklistedAlbum: BlacklistedAlbum)

    @Delete
    suspend fun removeBlacklistedAlbum(blacklistedAlbum: BlacklistedAlbum)

    @Query("SELECT EXISTS(SELECT * FROM blacklist WHERE id = :albumId)")
    fun albumIsBlacklisted(albumId: Long): Boolean

}