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
const val FAILED_MEDIA_DWELL_MILLIS = 1_500L

sealed interface SlideshowAdvance {
    data class Page(val index: Int) : SlideshowAdvance
    data object Hold : SlideshowAdvance
    data object Exit : SlideshowAdvance
}

/** Resolves the next action without ever producing an invalid pager index. */
fun resolveSlideshowAdvance(
    itemCount: Int,
    currentIndex: Int,
    loop: Boolean,
): SlideshowAdvance {
    if (itemCount <= 0) return SlideshowAdvance.Exit
    val safeIndex = currentIndex.coerceIn(0, itemCount - 1)
    if (safeIndex + 1 < itemCount) return SlideshowAdvance.Page(safeIndex + 1)
    if (!loop) return SlideshowAdvance.Exit
    return if (itemCount > 1) SlideshowAdvance.Page(0) else SlideshowAdvance.Hold
}

/** Images use the configured dwell; failed media gets a short readable error dwell before skip. */
fun slideshowDwellMillis(
    config: SlideshowConfig,
    isVideo: Boolean,
    loadFailed: Boolean,
): Long? = when {
    loadFailed -> FAILED_MEDIA_DWELL_MILLIS
    isVideo -> null
    else -> config.intervalMillis
}

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
