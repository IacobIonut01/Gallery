package com.dot.gallery.feature_node.presentation.mediaview

import org.junit.Assert.assertEquals
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

    @Test
    fun groupedMemberResolvesToItsRepresentativePageAndExactMember() {
        assertEquals(
            MediaViewerInitialSelection(pageIndex = 1, memberId = 22L, found = true),
            resolveMediaViewerInitialSelection(
                mediaId = 22L,
                pagerMediaIds = listOf(10L, 20L, 30L),
                mediaGroupIds = mapOf(20L to listOf(20L, 21L, 22L)),
            ),
        )
    }

    @Test
    fun completedViewerDismissesInsteadOfShowingRandomMediaWhenTargetIsMissing() {
        assertTrue(
            shouldDismissMissingMediaTarget(
                isLoading = false,
                targetFound = false,
                hasMedia = true,
                isStandalone = false,
            )
        )
        assertFalse(
            shouldDismissMissingMediaTarget(
                isLoading = true,
                targetFound = false,
                hasMedia = true,
                isStandalone = false,
            )
        )
    }
}
