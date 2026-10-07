package com.dot.gallery.domain.model

import androidx.room3.Entity
import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.domain.uri.UriSerializer
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "album_thumbnail", primaryKeys = ["albumId"])
data class AlbumThumbnail(
    val albumId: Long,
    @Serializable(with = UriSerializer::class)
    val thumbnailUri: Uri
)
