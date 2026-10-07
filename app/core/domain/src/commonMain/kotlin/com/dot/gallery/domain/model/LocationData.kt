package com.dot.gallery.domain.model


import androidx.compose.runtime.Stable

@Stable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val location: String
)
