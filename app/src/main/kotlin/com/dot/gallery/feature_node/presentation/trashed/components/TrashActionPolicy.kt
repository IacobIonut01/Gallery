/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.trashed.components

enum class TrashDialogAction {
    TRASH,
    DELETE,
    RESTORE
}

fun resolveTrashDialogAction(
    trashRequested: Boolean,
    trashEnabled: Boolean,
    trashSupported: Boolean
): TrashDialogAction = if (trashRequested && trashEnabled && trashSupported) {
    TrashDialogAction.TRASH
} else {
    TrashDialogAction.DELETE
}
