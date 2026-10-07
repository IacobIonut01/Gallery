package com.dot.gallery.domain.model

import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.domain.uri.parseUri

enum class MediaType(
    val externalContentUri: Uri,
    val mediaStoreValue: Int,
) {
    // URI/ints mirror MediaStore.Images.Media.EXTERNAL_CONTENT_URI and
    // MediaStore.Files.FileColumns.MEDIA_TYPE_* constants.
    IMAGE(
        parseUri("content://media/external/images/media"),
        1,
    ),
    VIDEO(
        parseUri("content://media/external/video/media"),
        3,
    );

    companion object {
        private val dashMimeTypes = listOf(
            "application/dash+xml",
        )

        private val hlsMimeTypes = listOf(
            "application/vnd.apple.mpegurl",
            "application/x-mpegurl",
            "audio/mpegurl",
            "audio/x-mpegurl",
        )

        private val smoothStreamingMimeTypes = listOf(
            "application/vnd.ms-sstr+xml",
        )

        fun fromMediaStoreValue(value: Int) = entries.first {
            value == it.mediaStoreValue
        }

        fun fromMimeType(mimeType: String) = when {
            mimeType.startsWith("image/") -> IMAGE
            mimeType.startsWith("video/") -> VIDEO
            mimeType in dashMimeTypes -> VIDEO
            mimeType in hlsMimeTypes -> VIDEO
            mimeType in smoothStreamingMimeTypes -> VIDEO
            else -> null
        }
    }
}
