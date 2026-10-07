/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.dot.gallery.domain.model.Media

/**
 * Click-time prefetch hook: warms the image pipeline's full-size cache before the viewer
 * opens the media. Android provides a Sketch enqueue; null elsewhere means no prefetch.
 */
fun interface MediaPrefetcher {
    fun prefetch(media: Media)
}

val LocalMediaPrefetcher = staticCompositionLocalOf<MediaPrefetcher?> { null }
