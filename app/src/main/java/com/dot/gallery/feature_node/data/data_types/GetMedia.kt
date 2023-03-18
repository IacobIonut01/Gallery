package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import java.io.File

fun ContentResolver.getMediaByType(mediaQuery: MediaQuery): List<Media> {
    val media = ArrayList<Media>()
    getCursor(mediaQuery)?.let { cursor ->
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            try {
                val isVideo = mediaQuery.uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val id: Long
                val path: String?
                val title: String?
                val albumID: Long
                val timestamp: Long
                val duration: String?
                if (isVideo) {
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    path =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    title =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                    albumID =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                    timestamp =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED))
                    duration =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                } else {
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    path =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    title =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                    albumID =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
                    timestamp =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                    duration = null
                }
                if (path != null && title != null) {
                    media.add(
                        Media(
                            id = id,
                            label = title,
                            uri = Uri.fromFile(File(path)),
                            path = path,
                            albumID = albumID,
                            timestamp = timestamp,
                            duration = duration
                        )
                    )
                }
            } catch(e: Exception) {
                cursor.close()
                e.printStackTrace()
            }
            cursor.moveToNext()
        }
        cursor.close()
    }
    return media
}

fun ContentResolver.getMedia(
    mediaQueries: List<MediaQuery>? = null,
    mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending)
): List<Media> {
    val media = ArrayList<Media>().also {
        if (mediaQueries == null) {
            // Add all media by default
            it.addAll(getImages(mediaOrder))
            it.addAll(getVideos(mediaOrder))
        } else {
            for (query in mediaQueries) {
                it.addAll(getMediaByType(query))
            }
        }
    }
    return mediaOrder.sortMedia(media)
}

fun ContentResolver.findMedia(
    mediaQueries: List<MediaQuery>? = null,
): Media? {
    val mediaList = getMedia(mediaQueries)
    return if (mediaList.isEmpty()) null else mediaList.first()
}