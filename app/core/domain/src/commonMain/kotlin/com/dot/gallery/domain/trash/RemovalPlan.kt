/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.trash

import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.isCloud

data class RemovalPlan<T : Media>(
    val trashable: List<T>,
    val deleteOnly: List<T>,
    val blocked: List<T>,
) {
    /** Everything a DELETE action may remove. */
    val targets: List<T> get() = trashable + deleteOnly
}

fun <T : Media> List<T>.planRemoval(
    isReadOnlyCloud: (T) -> Boolean,
    providerSupportsTrash: (T) -> Boolean,
    isCloud: (T) -> Boolean = { it.isCloud },
): RemovalPlan<T> {
    val trashable = mutableListOf<T>()
    val deleteOnly = mutableListOf<T>()
    val blocked = mutableListOf<T>()
    for (item in this) {
        when (
            removalClass(
                isCloud = isCloud(item),
                isReadOnlyCloud = isReadOnlyCloud(item),
                providerSupportsTrash = providerSupportsTrash(item),
            )
        ) {
            RemovalClass.TRASHABLE -> trashable += item
            RemovalClass.DELETE_ONLY -> deleteOnly += item
            RemovalClass.BLOCKED -> blocked += item
        }
    }
    return RemovalPlan(trashable, deleteOnly, blocked)
}
