package com.dot.gallery.feature_node.presentation.library

import androidx.compose.ui.graphics.Color
import com.dot.gallery.feature_node.presentation.library.components.LibraryShortcut
import com.dot.gallery.feature_node.presentation.library.components.LibraryShortcutPref
import com.dot.gallery.feature_node.presentation.library.components.LibraryShortcutSpan
import com.dot.gallery.feature_node.presentation.library.components.RuntimeShortcut
import com.dot.gallery.feature_node.presentation.library.components.mergeShortcutPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryShortcutTest {

    private val runtime = linkedMapOf(
        LibraryShortcut.TRASH to runtimeShortcut(LibraryShortcut.TRASH),
        LibraryShortcut.FAVORITES to runtimeShortcut(LibraryShortcut.FAVORITES),
        LibraryShortcut.VAULT to runtimeShortcut(LibraryShortcut.VAULT)
    )

    @Test
    fun emptyLayoutRestoresDefaultVisibilityAndOrder() {
        val merged = mergeShortcutPrefs(emptyList(), runtime)

        assertEquals(
            listOf(LibraryShortcut.TRASH, LibraryShortcut.FAVORITES, LibraryShortcut.VAULT),
            merged.map { it.shortcut }
        )
        assertTrue(merged.all { it.visible })
        assertTrue(merged.all { it.span == LibraryShortcutSpan.HALF })
    }

    @Test
    fun hiddenTrashRemainsInEditableLayout() {
        val merged = mergeShortcutPrefs(
            prefs = listOf(LibraryShortcutPref(id = LibraryShortcut.TRASH.id, visible = false)),
            runtime = runtime
        )

        val trash = merged.first { it.shortcut == LibraryShortcut.TRASH }
        assertFalse(trash.visible)
        assertEquals(3, merged.size)
    }

    @Test
    fun unknownAndDuplicateEntriesDoNotCorruptLayout() {
        val merged = mergeShortcutPrefs(
            prefs = listOf(
                LibraryShortcutPref(id = "unknown"),
                LibraryShortcutPref(id = LibraryShortcut.TRASH.id, span = LibraryShortcutSpan.FULL),
                LibraryShortcutPref(id = LibraryShortcut.TRASH.id)
            ),
            runtime = runtime
        )

        assertEquals(3, merged.size)
        assertEquals(LibraryShortcutSpan.FULL, merged.first().span)
        assertEquals(1, merged.count { it.shortcut == LibraryShortcut.TRASH })
    }

    private fun runtimeShortcut(shortcut: LibraryShortcut) = RuntimeShortcut(
        shortcut = shortcut,
        title = shortcut.id,
        icon = null,
        contentColor = Color.Unspecified,
        useIndicator = false,
        indicatorCounter = 0,
        route = shortcut.id,
        available = true
    )
}
