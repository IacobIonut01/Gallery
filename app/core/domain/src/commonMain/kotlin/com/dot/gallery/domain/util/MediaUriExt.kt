package com.dot.gallery.domain.util

import com.dot.gallery.domain.model.ClassifiedMedia
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.uri.Uri

fun <T : Media> T.getUri(): Uri {
    return when (this) {
        is UriMedia -> uri
        is ClassifiedMedia -> uri
        else -> throw IllegalArgumentException("Media type ${this::class.simpleName} not supported")
    }
}

fun <T : Media> T.asUriMedia(uri: Uri): UriMedia {
    return UriMedia(
        id = id,
        label = label,
        uri = uri,
        path = path,
        timestamp = timestamp,
        mimeType = mimeType,
        duration = duration,
        trashed = trashed,
        favorite = favorite,
        albumID = albumID,
        albumLabel = albumLabel,
        relativePath = relativePath,
        fullDate = fullDate,
        size = size,
    )
}
