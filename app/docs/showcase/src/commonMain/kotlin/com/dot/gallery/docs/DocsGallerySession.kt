/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import androidx.compose.runtime.compositionLocalOf
import com.dot.gallery.core.MediaSelector
import com.dot.gallery.core.MediaSelectorImpl
import com.dot.gallery.core.ScrollToTopController
import com.dot.gallery.domain.settings.SettingsStore

/**
 * Per-figure demo session: the in-memory media library and the providers real
 * screens read through composition locals. Created once per
 * (root scene, state, theme, seed, reset) in [DocsSceneHost] — internal
 * scene-to-scene navigation inside the host shares the same store, media and
 * selection so library edits survive navigating from a list into a detail and
 * back; the website Reset button remounts with a fresh token.
 */
class DocsGallerySession(
    val store: SettingsStore,
    val distributor: DocsMediaDistributor = DocsMediaDistributor(),
) {
    val selector: MediaSelector = MediaSelectorImpl()
    val scrollToTop = ScrollToTopController()
    val imageRenderer = DocsMediaImageRenderer
}

val LocalDocsGallerySession = compositionLocalOf<DocsGallerySession?> { null }
