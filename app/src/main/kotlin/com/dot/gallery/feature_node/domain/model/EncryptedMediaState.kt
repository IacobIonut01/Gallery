package com.dot.gallery.feature_node.domain.model

import androidx.compose.runtime.Stable

@Stable
data class EncryptedMediaState(
    val media: List<DecryptedMedia> = emptyList(),
    val mappedMedia: List<EncryptedMediaItem> = emptyList(),
    val mappedMediaWithMonthly: List<EncryptedMediaItem> = emptyList(),
    val headers: List<EncryptedMediaItem.Header> = emptyList(),
    val dateHeader: String = "",
    val error: String = "",
    val isLoading: Boolean = true
)