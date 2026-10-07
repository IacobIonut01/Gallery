/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.navigation

/**
 * Canonical route strings for startup-relevant destinations, shared between the
 * app-side `Screen` graph (which wraps them in typed destinations) and settings
 * code that must reason about routes without depending on the nav graph.
 */
object Routes {
    const val SETUP = "setup_screen"
    const val TIMELINE = "timeline_screen"
    const val ALBUMS = "albums_screen"
    const val LIBRARY = "library_screen"
}
