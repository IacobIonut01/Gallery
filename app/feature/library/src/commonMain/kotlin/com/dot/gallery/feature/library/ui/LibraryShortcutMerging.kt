/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui


/**
 * Merge the user's stored [prefs] with the [runtime] shortcuts.
 *
 * - Items present in [prefs] keep their stored order, span and visibility.
 * - Newly introduced shortcuts (not yet in [prefs]) are appended in their
 *   natural enum order, shown by default.
 * - Stored entries whose shortcut id is unknown are dropped.
 *
 * The result is ordered exactly as it should be rendered/edited.
 */
fun mergeShortcutPrefs(
    prefs: List<LibraryShortcutPref>,
    runtime: Map<LibraryShortcut, RuntimeShortcut>
): List<LibraryShortcutItem> {
    val byId = prefs.associateBy { it.id }
    val seen = LinkedHashSet<LibraryShortcut>()
    val result = ArrayList<LibraryShortcutItem>()

    // 1. Stored entries first, preserving their order.
    for (pref in prefs) {
        val shortcut = LibraryShortcut.fromId(pref.id) ?: continue
        if (!seen.add(shortcut)) continue
        val data = runtime[shortcut] ?: continue
        result += data.toItem(pref)
    }
    // 2. Append any runtime shortcut not yet persisted, in enum order.
    for (shortcut in LibraryShortcut.entries) {
        if (shortcut in seen) continue
        val data = runtime[shortcut] ?: continue
        val pref = byId[shortcut.id] ?: LibraryShortcutPref(id = shortcut.id)
        result += data.toItem(pref)
    }
    return result
}

/**
 * Pack span-aware [items] into rows of [columns] units. Items keep their order.
 * A new row is started whenever the next item would overflow the current row.
 *
 * Rendering uses weight, so every row always fills the full width regardless of
 * how many units it actually holds — this is what makes the layout adapt and
 * "fill the remaining space" instead of leaving the empty gaps the old
 * hard-coded two-column rows produced.
 */
fun <T> packShortcutRows(
    items: List<T>,
    columns: Int = LibraryShortcutSpan.COLUMNS,
    spanOf: (T) -> Int
): List<List<T>> {
    val rows = ArrayList<MutableList<T>>()
    var current = ArrayList<T>()
    var used = 0
    for (item in items) {
        val span = spanOf(item).coerceIn(1, columns)
        if (current.isNotEmpty() && used + span > columns) {
            rows += current
            current = ArrayList()
            used = 0
        }
        current += item
        used += span
    }
    if (current.isNotEmpty()) rows += current
    return rows
}
