package com.dot.gallery.core.util

import android.content.Intent
import android.provider.MediaStore
import com.dot.gallery.feature_node.domain.model.MediaType

object PickerUtils {
    private const val MIME_TYPE_IMAGE_ANY = "image/*"
    private const val MIME_TYPE_VIDEO_ANY = "video/*"
    private const val MIME_TYPE_ANY = "*/*"

    /**
     * Get a fixed up MIME type from an [Intent].
     * @param intent An [Intent]
     * @return A simpler MIME type, null if not supported
     */
    fun translateMimeType(intent: Intent?) = when (intent?.action) {
        Intent.ACTION_SET_WALLPAPER -> MIME_TYPE_IMAGE_ANY
        else -> (intent?.type ?: MIME_TYPE_ANY).let {
            when (it) {
                MediaStore.Images.Media.CONTENT_TYPE -> MIME_TYPE_IMAGE_ANY
                MediaStore.Video.Media.CONTENT_TYPE -> MIME_TYPE_VIDEO_ANY
                else -> when {
                    it == MIME_TYPE_ANY
                            || it.startsWith("image/")
                            || it.startsWith("video/") -> it

                    else -> null
                }
            }
        }
    }

    /**
     * Get a [MediaType] only if the provided MIME type is a generic one, else return null.
     * @param mimeType A MIME type
     * @return [MediaType] if the MIME type is generic, else null
     *         (assume MIME type represent either a specific file format or any)
     */
    fun mediaTypeFromGenericMimeType(mimeType: String?) = when (mimeType) {
        MIME_TYPE_IMAGE_ANY -> MediaType.IMAGE
        MIME_TYPE_VIDEO_ANY -> MediaType.VIDEO
        else -> null
    }

    /**
     * Given a MIME type, check if it specifies both a content type and a sub type.
     * @param mimeType A MIME type
     * @return true if it specifies both a file category and a specific type
     */
    fun isMimeTypeNotGeneric(mimeType: String?) = mimeType?.let {
        it !in listOf(
            MIME_TYPE_IMAGE_ANY,
            MIME_TYPE_VIDEO_ANY,
            MIME_TYPE_ANY,
        )
    } ?: false
}