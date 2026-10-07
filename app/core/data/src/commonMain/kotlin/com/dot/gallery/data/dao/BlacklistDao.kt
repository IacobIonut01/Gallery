package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert
import com.dot.gallery.domain.model.IgnoredAlbum
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