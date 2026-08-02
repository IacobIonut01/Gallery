package com.dot.gallery.feature_node.presentation.trashed

import com.dot.gallery.feature_node.presentation.trashed.components.TrashDialogAction
import com.dot.gallery.feature_node.presentation.trashed.components.resolveTrashDialogAction
import org.junit.Assert.assertEquals
import org.junit.Test

class TrashActionPolicyTest {

    @Test
    fun supportedEnabledTrashRequestIsRecoverable() {
        assertEquals(
            TrashDialogAction.TRASH,
            resolveTrashDialogAction(
                trashRequested = true,
                trashEnabled = true,
                trashSupported = true
            )
        )
    }

    @Test
    fun disabledTrashRequestIsPermanentDelete() {
        assertEquals(
            TrashDialogAction.DELETE,
            resolveTrashDialogAction(
                trashRequested = true,
                trashEnabled = false,
                trashSupported = true
            )
        )
    }

    @Test
    fun unsupportedTrashRequestIsPermanentDelete() {
        assertEquals(
            TrashDialogAction.DELETE,
            resolveTrashDialogAction(
                trashRequested = true,
                trashEnabled = true,
                trashSupported = false
            )
        )
    }

    @Test
    fun explicitDeleteNeverBecomesTrash() {
        assertEquals(
            TrashDialogAction.DELETE,
            resolveTrashDialogAction(
                trashRequested = false,
                trashEnabled = true,
                trashSupported = true
            )
        )
    }
}
