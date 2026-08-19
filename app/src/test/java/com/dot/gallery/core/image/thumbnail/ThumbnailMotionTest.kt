/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.image.thumbnail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailMotionTest {

    @Test
    fun movingGridUsesMotionTierForThumbnailWithoutRefinedLoad() {
        assertTrue(shouldUseMotionThumbnail(isMoving = true, hasLoadedRefined = false))
    }

    @Test
    fun movingGridKeepsCompletedRefinedThumbnail() {
        assertFalse(shouldUseMotionThumbnail(isMoving = true, hasLoadedRefined = true))
    }

    @Test
    fun idleGridUsesRefinedTier() {
        assertFalse(shouldUseMotionThumbnail(isMoving = false, hasLoadedRefined = false))
    }

    @Test
    fun refinedLoadTrackerRemembersCompletedThumbnail() {
        val tracker = RefinedThumbnailLoadTracker(capacity = 2)
        val key = RefinedThumbnailKey(model = "media-1", signature = "version-1")

        tracker.markLoaded(key)

        assertTrue(tracker.hasLoaded(key))
    }

    @Test
    fun refinedLoadTrackerSeparatesModelAndSignatureChanges() {
        val tracker = RefinedThumbnailLoadTracker(capacity = 3)
        tracker.markLoaded(RefinedThumbnailKey(model = "media-1", signature = "version-1"))

        assertFalse(tracker.hasLoaded(RefinedThumbnailKey("media-2", "version-1")))
        assertFalse(tracker.hasLoaded(RefinedThumbnailKey("media-1", "version-2")))
    }

    @Test
    fun refinedLoadTrackerEvictsLeastRecentlyUsedThumbnail() {
        val tracker = RefinedThumbnailLoadTracker(capacity = 2)
        val media1 = RefinedThumbnailKey(model = "media-1", signature = "version-1")
        val media2 = RefinedThumbnailKey(model = "media-2", signature = "version-1")
        val media3 = RefinedThumbnailKey(model = "media-3", signature = "version-1")
        tracker.markLoaded(media1)
        tracker.markLoaded(media2)
        tracker.hasLoaded(media1)

        tracker.markLoaded(media3)

        assertTrue(tracker.hasLoaded(media1))
        assertFalse(tracker.hasLoaded(media2))
        assertTrue(tracker.hasLoaded(media3))
    }
}
