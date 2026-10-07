/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui

import kotlinx.serialization.Serializable


/**
 * Persisted, user-editable layout entry for a single [LibraryShortcut].
 * The position in the stored list defines display order.
 */
@Serializable
data class LibraryShortcutPref(
    val id: String,
    val span: Int = LibraryShortcutSpan.HALF,
    val visible: Boolean = true
)
