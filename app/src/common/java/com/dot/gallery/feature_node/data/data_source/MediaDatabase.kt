package com.dot.gallery.feature_node.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dot.gallery.core.Converters
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media

@Database(
    entities = [Media::class, Album::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MediaDatabase: RoomDatabase() {

    abstract fun getMediaDao(): MediaDao

    abstract fun getAlbumDao(): AlbumDao

    companion object {
        const val DATABASE = "media_db"
    }
}