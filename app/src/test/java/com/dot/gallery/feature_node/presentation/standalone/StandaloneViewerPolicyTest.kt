package com.dot.gallery.feature_node.presentation.standalone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StandaloneViewerPolicyTest {

    @Test
    fun videoIntentStartsWithViewerUiHidden() {
        assertFalse(
            shouldShowStandaloneViewerUi(
                intentMimeType = "video/mp4",
                contentMimeType = null,
            )
        )
    }

    @Test
    fun resolvedVideoTypeStartsWithViewerUiHidden() {
        assertFalse(
            shouldShowStandaloneViewerUi(
                intentMimeType = "application/octet-stream",
                contentMimeType = "video/mp4",
            )
        )
    }

    @Test
    fun imageIntentKeepsViewerUiVisible() {
        assertTrue(
            shouldShowStandaloneViewerUi(
                intentMimeType = "image/jpeg",
                contentMimeType = "image/jpeg",
            )
        )
    }
}
