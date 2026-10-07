/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.mediaview

import androidx.compose.ui.graphics.vector.ImageVector
import com.dot.gallery.ui.core.icons.GoogleLens
import com.dot.gallery.ui.core.icons.VisualSearch
import com.dot.gallery.ui.core.Icons as GalleryIcons

/** One installed activity that can receive an image share. */
data class VisualSearchTarget(
    val packageName: String,
    val activityName: String,
    val label: String,
    val known: KnownVisualSearchProvider?,
) {
    /** Brand name for known providers (the app label can be terse, e.g. "Lens"), else app label. */
    val displayName: String get() = known?.brandName ?: label
}

/** Button glyph: the true brand-colored Lens icon when Lens is selected, generic otherwise. */
val VisualSearchTarget.icon: ImageVector
    get() = if (known == KnownVisualSearchProvider.GOOGLE_LENS) {
        GalleryIcons.GoogleLens
    } else {
        GalleryIcons.VisualSearch
    }

/**
 * Whether the button glyph should be tinted to the surrounding content color. The Lens glyph
 * carries brand colors and renders untinted; the generic icon is monochrome and tinted.
 */
val VisualSearchTarget.tintIcon: Boolean
    get() = known != KnownVisualSearchProvider.GOOGLE_LENS

/**
 * Picks the effective target: the stored package when it is still installed, otherwise the best
 * auto choice (the sorted list already puts Lens first). `null` when nothing is installed.
 */
fun resolveVisualSearchTarget(
    targets: List<VisualSearchTarget>,
    storedPackage: String,
): VisualSearchTarget? {
    if (targets.isEmpty()) return null
    if (storedPackage.isNotEmpty()) {
        targets.firstOrNull { it.packageName == storedPackage }?.let { return it }
    }
    return targets.first()
}
