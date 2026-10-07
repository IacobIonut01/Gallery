/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui

import kotlinx.serialization.Serializable


@Serializable
data class LibraryScrollPosition(
    val key: String? = null,
    val index: Int = 0,
    val offset: Int = 0
)

fun LibraryScrollPosition.isMeasuredPosition(): Boolean =
    key != null || index != 0 || offset != 0

fun restoredLibraryIndex(keys: List<String>, position: LibraryScrollPosition): Int =
    keys.indexOf(position.key).takeIf { it >= 0 }
        ?: position.index.coerceIn(0, (keys.size - 1).coerceAtLeast(0))

fun <T> libraryPreviewWindow(
    items: List<T>,
    position: LibraryScrollPosition,
    key: (T) -> String
): List<T> {
    if (items.size <= 64) return items
    val anchor = restoredLibraryIndex(items.map(key), position)
    val start = (anchor - 16).coerceIn(0, items.size - 64)
    return items.subList(start, start + 64).toList()
}
