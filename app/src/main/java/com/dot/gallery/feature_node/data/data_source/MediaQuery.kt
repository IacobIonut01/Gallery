package com.dot.gallery.feature_node.data.data_source

import android.net.Uri
import android.provider.MediaStore

sealed class MediaQuery(
    var uri: Uri,
    var projection: Array<String>? = null,
    var selection: String? = null,
    var selectionArgs: Array<String>? = null,
    var sortOrder: String? = null
) {

    object PhotoQuery : MediaQuery(
        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.DATE_MODIFIED
        )
    )

    object VideoQuery : MediaQuery(
        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION
        )
    )

    fun copy(
        uri: Uri = this.uri,
        projection: Array<String>? = this.projection,
        selection: String? = this.selection,
        selectionArgs: Array<String>? = this.selectionArgs,
        sortOrder: String? = this.sortOrder,
    ): MediaQuery {
        this.uri = uri
        this.projection = projection
        this.selection = selection
        this.selectionArgs = selectionArgs
        this.sortOrder = sortOrder
        return this
    }

}