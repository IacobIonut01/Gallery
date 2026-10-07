package com.dot.gallery.domain.model

import androidx.compose.runtime.Stable


@Stable
data class MediaDateCaption(
    val date: String,
    val deviceInfo: String? = null,
    val description: String
)
