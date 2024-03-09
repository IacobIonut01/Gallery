package com.dot.gallery.feature_node.domain.model

data class PlaybackSpeed(
    val speed: Float,
    val label: String,
    val isAuto: Boolean = false
)
