package com.dot.gallery.domain.model

import androidx.compose.runtime.Stable

@Stable
data class GeoMedia(
    val mediaId: Long,
    val latitude: Double,
    val longitude: Double,
    val locationCity: String?,
    val locationCountry: String?,
    val media: UriMedia
)
