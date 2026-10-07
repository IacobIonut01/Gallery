/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui

import com.dot.gallery.domain.settings.PrefKey
import kotlinx.serialization.builtins.ListSerializer


/**
 * Persisted Library shortcut layout key — `List<LibraryShortcutPref>` JSON.
 * Same store row as the app-side `Settings.Library.SHORTCUTS_LAYOUT` /
 * `rememberShortcutsLayout` (`"library_shortcuts_layout"`); the codecs are
 * identical (`PrefKey.serializable` == `rememberPreferenceSerializable`).
 */
val libraryShortcutsLayoutKey: PrefKey<List<LibraryShortcutPref>> = PrefKey.serializable(
    name = "library_shortcuts_layout",
    default = emptyList(),
    serializer = ListSerializer(LibraryShortcutPref.serializer())
)
