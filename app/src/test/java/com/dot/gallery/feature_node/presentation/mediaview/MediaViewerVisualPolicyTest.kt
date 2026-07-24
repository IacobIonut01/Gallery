package com.dot.gallery.feature_node.presentation.mediaview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaViewerVisualPolicyTest {

    @Test
    fun blurAlwaysUsesDarkBackground() {
        val policy = MediaViewerVisualPolicy(allowBlur = true)

        assertTrue(policy.usesDarkBackground(isDarkTheme = false))
        assertTrue(policy.usesDarkBackground(isDarkTheme = true))
    }

    @Test
    fun disabledBlurFollowsTheme() {
        val policy = MediaViewerVisualPolicy(allowBlur = false)

        assertFalse(policy.usesDarkBackground(isDarkTheme = false))
        assertTrue(policy.usesDarkBackground(isDarkTheme = true))
    }
}
