package com.dot.gallery.feature_node.data.data_source

import android.net.Uri

data class MediaObject(
    val uri: Uri,
    val projection: Array<String>? = null,
    val selection: String? = null,
    val selectionArgs: Array<String>? = null,
    val sortOrder: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaObject

        if (uri != other.uri) return false
        if (projection != null) {
            if (other.projection == null) return false
            if (!projection.contentEquals(other.projection)) return false
        } else if (other.projection != null) return false
        if (selection != other.selection) return false
        if (selectionArgs != null) {
            if (other.selectionArgs == null) return false
            if (!selectionArgs.contentEquals(other.selectionArgs)) return false
        } else if (other.selectionArgs != null) return false
        if (sortOrder != other.sortOrder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + (projection?.contentHashCode() ?: 0)
        result = 31 * result + (selection?.hashCode() ?: 0)
        result = 31 * result + (selectionArgs?.contentHashCode() ?: 0)
        result = 31 * result + (sortOrder?.hashCode() ?: 0)
        return result
    }
}