package com.dot.gallery.feature_node.domain.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class LibraryIndicatorState(
    val trashCount: Int = 0,
    val favoriteCount: Int = 0,
)
