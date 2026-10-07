/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.location

enum class MapAppearance(val storedValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun resolvesDark(effectiveAppIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> effectiveAppIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStored(value: String): MapAppearance =
            entries.firstOrNull { it.storedValue == value } ?: SYSTEM
    }
}
