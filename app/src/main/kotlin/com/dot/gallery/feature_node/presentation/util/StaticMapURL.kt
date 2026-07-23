/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.util

import com.dot.gallery.feature_node.presentation.location.MapAppearance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Generates a static map tile URL for a given lat/lng.
 * No API key required. Uses Carto CDN basemap tiles (light/dark).
 */
object StaticMapURL {

    private const val CARTO_LIGHT = "https://basemaps.cartocdn.com/rastertiles/voyager"
    private const val CARTO_DARK = "https://basemaps.cartocdn.com/rastertiles/dark_all"

    operator fun invoke(
        latitude: Double,
        longitude: Double,
        appearance: MapAppearance = MapAppearance.SYSTEM,
        effectiveAppIsDark: Boolean = false,
        zoom: Int = 12,
    ): String {
        val safeZoom = zoom.coerceIn(0, 20)
        val x = lonToTileX(longitude, safeZoom)
        val y = latToTileY(latitude, safeZoom)
        val base = if (appearance.resolvesDark(effectiveAppIsDark)) CARTO_DARK else CARTO_LIGHT
        return "$base/$safeZoom/$x/$y@2x.png"
    }

    internal fun lonToTileX(longitude: Double, zoom: Int): Int {
        val count = 1 shl zoom.coerceIn(0, 20)
        val normalized = ((longitude + 180.0) % 360.0 + 360.0) % 360.0
        return floor(normalized / 360.0 * count).toInt().coerceIn(0, count - 1)
    }

    internal fun latToTileY(latitude: Double, zoom: Int): Int {
        val count = 1 shl zoom.coerceIn(0, 20)
        val lat = latitude.coerceIn(-85.05112878, 85.05112878)
        val latRad = Math.toRadians(lat)
        return floor(
            (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * count
        ).toInt().coerceIn(0, count - 1)
    }

    internal fun tileYToLatitude(y: Int, zoom: Int): Double {
        val count = 1 shl zoom.coerceIn(0, 20)
        return Math.toDegrees(kotlin.math.atan(sinh(PI * (1.0 - 2.0 * y / count))))
    }
}