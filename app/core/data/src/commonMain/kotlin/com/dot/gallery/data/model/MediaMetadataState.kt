package com.dot.gallery.data.model

import com.dot.gallery.data.entity.MediaMetadata

data class MediaMetadataState(
    val metadata: List<MediaMetadata> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingProgress: Int = 0,
) {
    val metadataMap: Map<Long, MediaMetadata> by lazy {
        metadata.associateBy { it.mediaId }
    }
}
