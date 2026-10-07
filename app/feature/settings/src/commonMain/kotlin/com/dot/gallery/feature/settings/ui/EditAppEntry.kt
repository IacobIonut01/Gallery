/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

/** Portable stand-in for `ResolveInfo` in the default-editor picker: package + display label. */
data class EditAppEntry(
    val packageName: String,
    val label: String,
)
