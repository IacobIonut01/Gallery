package com.dot.gallery.app.presentation.search

import com.dot.gallery.feature.classifier.ui.search.shouldClearSearchOnBack
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.cloud.core.matches
import com.dot.gallery.domain.uri.query



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
