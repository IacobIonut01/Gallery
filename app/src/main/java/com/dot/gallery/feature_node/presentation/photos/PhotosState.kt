package com.dot.gallery.feature_node.presentation.photos

import com.dot.gallery.feature_node.domain.model.Media

data class PhotosState(
    val isLoading: Boolean = false,
    val media: List<Media> = emptyList(),
    val error: String = ""
)