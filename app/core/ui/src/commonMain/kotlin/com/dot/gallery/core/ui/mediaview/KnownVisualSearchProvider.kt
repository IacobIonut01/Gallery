/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.mediaview

/**
 * Visual-search providers are ordinary apps that accept `ACTION_SEND` image shares — there is no
 * dedicated intent, so discovery and the picker both run on the share-target query. A curated
 * catalog gives the well-known visual-search apps a friendly name, brand ordering and (for Lens)
 * the monochrome glyph; every other installed share target remains selectable under its app label.
 */
enum class KnownVisualSearchProvider(val packageName: String, val brandName: String) {
    GOOGLE_LENS("com.google.ar.lens", "Google Lens"),
    GOOGLE_APP("com.google.android.googlequicksearchbox", "Google"),
    BING("com.microsoft.bing", "Bing"),
    YANDEX("ru.yandex.searchplugin", "Yandex"),
    PINTEREST("com.pinterest", "Pinterest");

    companion object {
        fun forPackage(packageName: String): KnownVisualSearchProvider? =
            entries.firstOrNull { it.packageName == packageName }
    }
}
