/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.location

object MapStyles {
    const val OPEN_FREE_MAP_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
    const val OPEN_FREE_MAP_DARK = "https://tiles.openfreemap.org/styles/dark"

    fun interactiveStyle(appearance: MapAppearance, effectiveAppIsDark: Boolean): String =
        if (appearance.resolvesDark(effectiveAppIsDark)) OPEN_FREE_MAP_DARK else OPEN_FREE_MAP_LIGHT
}
