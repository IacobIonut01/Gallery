package com.dot.gallery.feature_node.presentation.albums.components

internal enum class AlbumThumbnailPresentation(
    val allowsMediaRequest: Boolean
) {
    MEDIA(allowsMediaRequest = true),
    LOCKED_PLACEHOLDER(allowsMediaRequest = false)
}

internal fun albumThumbnailPresentation(isLocked: Boolean): AlbumThumbnailPresentation =
    if (isLocked) {
        AlbumThumbnailPresentation.LOCKED_PLACEHOLDER
    } else {
        AlbumThumbnailPresentation.MEDIA
    }
