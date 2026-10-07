/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dot.gallery.domain.util.getLocation
import com.dot.gallery.core.presentation.util.rememberGeocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.dot.gallery.domain.model.LocationData
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.domain.util.formattedAddress
import com.dot.gallery.data.entity.formattedCords
import com.dot.gallery.data.entity.isRedactedCoordinate


@Composable
fun rememberLocationData(
    exifMetadata: MediaMetadata?
): LocationData? {
    val geocoder = rememberGeocoder()
    var locationName by remember { mutableStateOf(exifMetadata?.formattedCords) }
    LaunchedEffect(geocoder, exifMetadata) {
        withContext(Dispatchers.IO) {
            val latitude = exifMetadata?.gpsLatitude
            val longitude = exifMetadata?.gpsLongitude
            if (latitude != null && longitude != null &&
                !isRedactedCoordinate(latitude, longitude)
            ) {
                geocoder?.getLocation(
                    latitude,
                    longitude
                ) { address ->
                    address?.let {
                        val addressName = it.formattedAddress
                        if (addressName.isNotEmpty()) {
                            locationName = addressName
                        }
                    }
                }
            }
        }
    }
    return remember(exifMetadata, locationName) {
        exifMetadata?.let {
            val lat = it.gpsLatitude
            val lon = it.gpsLongitude
            if (lat == null || lon == null ||
                isRedactedCoordinate(lat, lon)
            ) return@let null
            LocationData(
                latitude = lat,
                longitude = lon,
                location = locationName ?: "Unknown"
            )
        }
    }
}