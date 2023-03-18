package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.MediaQuery
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.util.MediaOrder

fun ContentResolver.getAlbums(mediaOrder: MediaOrder): List<Album> {
    val albums = ArrayList<Album>()
    val imagesQuery = MediaQuery.PhotoQuery().copy(
        projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_MODIFIED
        ),
        sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
    )
    val videosQuery = MediaQuery.VideoQuery().copy(
        projection = arrayOf(
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_MODIFIED
        ),
        sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
    )
    getCursor(imagesQuery)?.let {
        it.moveToFirst()
        while (!it.isAfterLast) {
            try {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
                val label =
                    it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                val thumbnailPath =
                    it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                val thumbnailDate =
                    it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                val album = Album(id, label, thumbnailPath, thumbnailDate, count = 1)
                val currentAlbum = albums.find { albm -> albm.id == id }
                if (currentAlbum == null)
                    albums.add(album)
                else {
                    val i = albums.indexOf(currentAlbum)
                    albums[i].count++
                    if (albums[i].timestamp <= thumbnailDate) {
                        album.count = albums[i].count
                        albums[i] = album
                    }
                }

            } catch (e: Exception) {
                it.close()
                e.printStackTrace()
            }
            it.moveToNext()
        }
        it.close()
    }
    getCursor(videosQuery)?.let {
        it.moveToFirst()
        while (!it.isAfterLast) {
            try {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                val label =
                    it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                val thumbnailPath =
                    it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                val thumbnailDate =
                    it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED))
                val album = Album(id, label, thumbnailPath, thumbnailDate, count = 1)
                val currentAlbum = albums.find { albm -> albm.id == id }
                if (currentAlbum == null)
                    albums.add(album)
                else {
                    val i = albums.indexOf(currentAlbum)
                    albums[i].count++
                    if (albums[i].timestamp <= thumbnailDate) {
                        album.count = albums[i].count
                        albums[i] = album
                    }
                }
            } catch (e: Exception) {
                it.close()
                e.printStackTrace()
            }
            it.moveToNext()
        }
        it.close()
    }
    return mediaOrder.sortAlbums(albums)
}