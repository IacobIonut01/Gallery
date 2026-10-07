package com.dot.gallery.domain.model

import androidx.compose.runtime.Stable

@Stable
data class MediaState<Type : Media>(
    val media: List<Type> = emptyList(),
    val pagerMedia: List<Type> = emptyList(),
    val mediaGroups: Map<Long, List<Type>> = emptyMap(),
    val mappedMedia: List<MediaItem<Type>> = emptyList(),
    val mappedMediaWithMonthly: List<MediaItem<Type>> = emptyList(),
    val mappedMediaWithYearly: List<MediaItem<Type>> = emptyList(),
    val headers: List<MediaItem.Header<Type>> = emptyList(),
    val dateHeader: String = "",
    val dateSource: TimelineDateSource = TimelineDateSource.CAPTURE_TIME,
    /**
     * Maps a local media id to the cloud copies that back it up (one entry per cloud
     * provider/account holding a matching asset). Populated only for the unified timeline
     * so the grid can show a backup indicator and the viewer can list backup destinations.
     * Cloud copies mapped here are skipped from [media]/[pagerMedia] (no merging).
     */
    val cloudBackups: Map<Long, List<UriMedia>> = emptyMap(),
    val error: String = "",
    val isLoading: Boolean = true,
    val isPartial: Boolean = false
)

fun MediaState<*>.isCompleteForLibrary(): Boolean =
    !isLoading && !isPartial && error.isEmpty()