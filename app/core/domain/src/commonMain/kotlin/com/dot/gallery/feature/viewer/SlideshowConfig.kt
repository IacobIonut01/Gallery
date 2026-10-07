/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

import androidx.compose.runtime.Stable
import com.dot.gallery.domain.model.SlideshowTransition

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
