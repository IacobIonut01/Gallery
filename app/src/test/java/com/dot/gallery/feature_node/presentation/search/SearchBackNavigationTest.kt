package com.dot.gallery.feature_node.presentation.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchBackNavigationTest {
    @Test
    fun activeTextQueryIsClearedOnBackWithoutDependingOnResults() {
        assertTrue(shouldClearSearchOnBack(query = "no matches", hasSelectedImage = false))
    }

    @Test
    fun selectedImageSearchIsClearedOnBack() {
        assertTrue(shouldClearSearchOnBack(query = "", hasSelectedImage = true))
    }

    @Test
    fun inactiveSearchAllowsBackNavigation() {
        assertFalse(shouldClearSearchOnBack(query = "", hasSelectedImage = false))
    }
}
