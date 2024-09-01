package com.dot.gallery.feature_node.domain.model

import androidx.compose.runtime.Stable

@Stable
data class MediaState(
    val media: List<Media> = emptyList(),
    val mappedMedia: List<MediaItem> = emptyList(),
    val mappedMediaWithMonthly: List<MediaItem> = emptyList(),
    val headers: List<MediaItem.Header> = emptyList(),
    val dateHeader: String = "",
    val error: String = "",
    val isLoading: Boolean = true
)