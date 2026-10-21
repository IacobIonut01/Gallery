/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import com.dot.gallery.data.entity.MediaMetadata

/**
 * Returns the most frequent non-blank city among [cities], or null when none have geo.
 * Ties are broken lexicographically for stable UI.
 */
fun dominantCity(cities: Iterable<String?>): String? {
    val counts = HashMap<String, Int>()
    for (city in cities) {
        val trimmed = city?.trim().orEmpty()
        if (trimmed.isEmpty()) continue
        counts[trimmed] = (counts[trimmed] ?: 0) + 1
    }
    return counts.entries.maxWithOrNull(
        compareBy<Map.Entry<String, Int>>({ it.value }).thenByDescending { it.key }
    )?.key
}

/**
 * Dominant city for a day's media from the unified metadata map
 * (local EXIF reverse-geocode + cloud city via the metadata pipeline).
 */
fun dominantCityForMediaIds(
    mediaIds: Set<Long>,
    metadataMap: Map<Long, MediaMetadata>,
): String? = dominantCity(mediaIds.map { metadataMap[it]?.gpsLocationNameCity })
