/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.location

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng


/**
 * Lightweight camera data class for Compose state.
 */
data class GalleryCameraPosition(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val zoom: Double = 2.0,
    val tilt: Double = 0.0,
    val bearing: Double = 0.0,
    val paddingLeft: Double = 0.0,
    val paddingTop: Double = 0.0,
    val paddingRight: Double = 0.0,
    val paddingBottom: Double = 0.0,
)

fun GalleryCameraPosition.toNative(): CameraPosition {
    return CameraPosition.Builder()
        .target(LatLng(latitude, longitude))
        .zoom(zoom)
        .tilt(tilt)
        .bearing(bearing)
        .padding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        .build()
}
