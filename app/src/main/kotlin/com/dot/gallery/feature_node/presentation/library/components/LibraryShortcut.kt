/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.library.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * Stable identifiers for every shortcut tile that can appear in the Library
 * shortcuts area. Persistence keys off of [id], so values must never change
 * once shipped (the enum order may change freely — order is persisted
 * separately by the user's layout).
 */
enum class LibraryShortcut(val id: String) {
    TRASH("trash"),
    FAVORITES("favorites"),
    VAULT("vault"),
    IGNORED("ignored"),
    PRIVATE_FOLDER("private_folder"),
    CLOUD_ARCHIVE("cloud_archive"),
    CLOUD_SHARED_LINKS("cloud_shared_links"),
    CLOUD_BACKUP("cloud_backup"),
    CLOUD_ACCOUNTS("cloud_accounts");

    companion object {
        fun fromId(id: String): LibraryShortcut? = entries.firstOrNull { it.id == id }
    }
}

object LibraryShortcutSpan {
    /** Half of the row (shares a row with another half tile). */
    const val HALF = 1

    /** The whole row. */
    const val FULL = 2

    /** Total column units per row. */
    const val COLUMNS = 2
}

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

/**
 * Fully resolved shortcut ready to render: persisted layout ([pref]) merged
 * with the live data (title/icon/colour/count/route) computed at runtime.
 *
 * [available] reflects whether the underlying feature is currently usable
 * (e.g. cloud connected, private folder configured). Unavailable shortcuts are
 * never rendered, but their layout preference is preserved so they reappear in
 * their previous slot once the feature comes back.
 */
@Immutable
data class LibraryShortcutItem(
    val shortcut: LibraryShortcut,
    val title: String,
    val icon: ImageVector?,
    val contentColor: Color,
    val useIndicator: Boolean,
    val indicatorCounter: Int,
    val route: String,
    val available: Boolean,
    val pref: LibraryShortcutPref
) {
    val span: Int get() = pref.span
    val visible: Boolean get() = pref.visible
}

/**
 * Merge the user's stored [prefs] with the [available] runtime shortcuts.
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
 * Runtime data for a shortcut, independent of the persisted layout.
 */
@Immutable
data class RuntimeShortcut(
    val shortcut: LibraryShortcut,
    val title: String,
    val icon: ImageVector?,
    val contentColor: Color,
    val useIndicator: Boolean,
    val indicatorCounter: Int,
    val route: String,
    val available: Boolean
) {
    fun toItem(pref: LibraryShortcutPref) = LibraryShortcutItem(
        shortcut = shortcut,
        title = title,
        icon = icon,
        contentColor = contentColor,
        useIndicator = useIndicator,
        indicatorCounter = indicatorCounter,
        route = route,
        available = available,
        pref = pref
    )
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
