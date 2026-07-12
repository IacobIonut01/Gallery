/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.slideshow

import androidx.compose.runtime.Stable
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.SlideshowTransition
import com.dot.gallery.feature_node.domain.util.isVideo
import kotlin.random.Random

/**
 * Immutable configuration for a slideshow session. Populated from the persisted
 * `Settings.Slideshow.*` values when the slideshow starts.
 */
@Stable
data class SlideshowConfig(
    val intervalSeconds: Int = 5,
    val random: Boolean = false,
    val reverse: Boolean = false,
    val includeGifs: Boolean = true,
    val includeVideos: Boolean = true,
    val loop: Boolean = true,
    val transition: SlideshowTransition = SlideshowTransition.FADE,
    val kenBurns: Boolean = false
) {
    val intervalMillis: Long get() = intervalSeconds.coerceAtLeast(1).toLong() * 1000L
}

private const val GIF_MIME = "image/gif"

/**
 * Returns true if [media] should be shown for the given [config] type filters.
 */
fun Media.matchesSlideshowFilters(config: SlideshowConfig): Boolean {
    if (isVideo && !config.includeVideos) return false
    if (mimeType == GIF_MIME && !config.includeGifs) return false
    return true
}

/**
 * Builds the ordered playlist that drives the slideshow pager.
 *
 * The result is filtered by [SlideshowConfig] type toggles, optionally reversed and/or
 * shuffled (deterministically for a given [seed]), and rotated so it begins at [startId]
 * when that item survives the filters. The slideshow pager follows this exact order so
 * "advance" is always the next index (wrapping to 0 at the end).
 *
 * Pure function — safe to unit test.
 */
fun <T : Media> buildSlideshowOrder(
    source: List<T>,
    config: SlideshowConfig,
    startId: Long?,
    seed: Long
): List<T> {
    val filtered = source
        .distinctBy { it.id }
        .filter { it.matchesSlideshowFilters(config) }
    if (filtered.isEmpty()) return emptyList()

    // Locate the requested start item (before any reordering) so we can keep it first.
    val startItem = startId?.let { id -> filtered.firstOrNull { it.id == id } }

    if (config.random) {
        val rest = filtered.filter { it.id != startItem?.id }
        val shuffled = rest.shuffled(Random(seed))
        return if (startItem != null) listOf(startItem) + shuffled else shuffled
    }

    val ordered = if (config.reverse) filtered.asReversed().toList() else filtered

    if (startItem == null) return ordered
    val startIndex = ordered.indexOfFirst { it.id == startItem.id }
    if (startIndex <= 0) return ordered
    // Rotate so the playlist begins at the tapped item.
    return ordered.subList(startIndex, ordered.size) + ordered.subList(0, startIndex)
}
