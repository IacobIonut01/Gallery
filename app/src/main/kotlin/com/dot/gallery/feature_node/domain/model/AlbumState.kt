package com.dot.gallery.feature_node.domain.model

import androidx.compose.runtime.Stable

@Stable
data class AlbumState(
    val albums: List<Album> = emptyList(),
    val albumsWithBlacklisted: List<Album> = emptyList(),
    val albumsUnpinned: List<Album> = emptyList(),
    val albumsPinned: List<Album> = emptyList(),
    val albumGroups: List<AlbumGroupWithAlbums> = emptyList(),
    val albumSections: List<AlbumSectionWithAlbums> = emptyList(),
    val collections: List<CollectionWithCount> = emptyList(),
    val error: String = "",
    val isLoading: Boolean = true
)