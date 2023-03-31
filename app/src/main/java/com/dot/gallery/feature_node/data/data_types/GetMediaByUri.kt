package com.dot.gallery.feature_node.data.data_types

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.dot.gallery.feature_node.data.data_source.MediaQuery
import com.dot.gallery.feature_node.domain.model.Media
import java.io.File

fun ContentResolver.getMediaByUri(uri: Uri): Media? {
    var media: Media? = null
    val mediaQueries = listOf(
        MediaQuery.PhotoQuery(),
        MediaQuery.VideoQuery()
    )
    val bundle = Bundle().apply {
        putString(
            ContentResolver.QUERY_ARG_SQL_SELECTION,
            MediaStore.MediaColumns.DATA + "=?"
        )
        putStringArray(
            ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
            arrayOf(uri.toString())
        )
    }
    for (mediaQuery in mediaQueries) {
        getCursor(mediaQuery, bundle)?.let { cursor ->
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                try {
                    val isVideo = mediaQuery.uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    val id: Long
                    val path: String?
                    val title: String?
                    val albumID: Long
                    var albumLabel: String
                    val timestamp: Long
                    val orientation: Int
                    val mimeType: String
                    val isFavorite: Int
                    val isTrashed: Int
                    val duration: String?
                    if (isVideo) {
                        id =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                        path =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                        title =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                        albumID =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                        albumLabel = try {
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                        } catch (e: Exception) {
                            Build.MODEL
                        }
                        timestamp =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED))
                        duration =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                        orientation =
                            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ORIENTATION))
                        mimeType =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE))
                        isFavorite =
                            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.IS_FAVORITE))
                        isTrashed =
                            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.IS_TRASHED))
                    } else {
                        id =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        path =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                        title =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        albumID =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
                        albumLabel = try {
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                        } catch (e: Exception) {
                            Build.MODEL
                        }
                        timestamp =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                        orientation =
                            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION))
                        mimeType =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                        isFavorite =
                            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_FAVORITE))
                        isTrashed =
                            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_TRASHED))
                        duration = null
                    }
                    if (path != null && title != null) {
                        media = Media(
                            id = id,
                            label = title,
                            uri = Uri.fromFile(File(path)),
                            path = path,
                            albumID = albumID,
                            albumLabel = albumLabel,
                            timestamp = timestamp,
                            duration = duration,
                            favorite = isFavorite,
                            trashed = isTrashed,
                            orientation = orientation,
                            mimeType = mimeType
                        )
                        break
                    }
                } catch (e: Exception) {
                    cursor.close()
                    e.printStackTrace()
                }
                cursor.moveToNext()
            }
            cursor.close()
        }
    }
    return media
}