package com.dot.gallery.domain.trash

import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.isEncrypted
import com.dot.gallery.domain.util.isTrashed

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

/**
 * Whether this item can be moved to a *recoverable* trash, so a gesture may act on it without
 * ever falling back to a permanent delete. [trashEnabled] and [trashSupported] are supplied by
 * the caller per item (a cloud item's bin comes from its provider, a local one from the platform
 * and the Trash setting), keeping this predicate free of platform lookups.
 */
fun Media.canMoveToTrashRecoverably(
    trashEnabled: Boolean,
    trashSupported: Boolean,
): Boolean = !isTrashed &&
        !isEncrypted &&
        resolveTrashDialogAction(
            trashRequested = true,
            trashEnabled = trashEnabled,
            trashSupported = trashSupported,
        ) == TrashDialogAction.TRASH
