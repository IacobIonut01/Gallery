/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

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
