package com.dot.gallery.app.presentation.storycards

import com.dot.gallery.domain.model.StoryCard
import com.dot.gallery.domain.model.StoryCardType
import com.dot.gallery.domain.model.StoryCardsConfig
import com.dot.gallery.feature.viewer.shouldCommitViewerDismiss
import com.dot.gallery.feature.viewer.viewerDismissProgress
import java.time.MonthDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.feature.timeline.storycards.annualDayDistance
import com.dot.gallery.feature.timeline.storycards.createStoryViewerSnapshot
import com.dot.gallery.feature.timeline.storycards.storyCardStableId



class StoryCardsPolicyTest {

    @Test
    fun viewerSnapshot_requiresRequestedCardAndFreezesOrder() {
        val cards = mutableListOf(
            storyCard(10L, StoryCardType.MEMORIES),
            storyCard(20L, StoryCardType.ALBUMS),
        )

        assertNull(createStoryViewerSnapshot(null, 10L))
        assertNull(createStoryViewerSnapshot(emptyList(), 10L))
        assertNull(createStoryViewerSnapshot(cards, 30L))

        val snapshot = createStoryViewerSnapshot(cards, 20L)
        assertNotNull(snapshot)
        cards.reverse()

        assertEquals(20L, snapshot!!.initialCardId)
        assertEquals(listOf(10L, 20L), snapshot.cards.map { it.id })
    }

    @Test
    fun normalizedOrder_removesDuplicatesAndAppendsMissingTypes() {
        val config = StoryCardsConfig(
            cardOrder = listOf(
                StoryCardType.ALBUMS,
                StoryCardType.ALBUMS,
                StoryCardType.MEMORIES,
            )
        )

        assertEquals(
            listOf(
                StoryCardType.ALBUMS,
                StoryCardType.MEMORIES,
                StoryCardType.CATEGORIES,
                StoryCardType.LOCATIONS,
                StoryCardType.FAVORITES,
                StoryCardType.CLOUD_MEMORIES,
            ),
            config.normalizedOrder,
        )
    }

    @Test
    fun stableId_isDeterministicAndNamespacedByType() {
        val first = storyCardStableId(StoryCardType.ALBUMS, "42")

        assertEquals(first, storyCardStableId(StoryCardType.ALBUMS, "42"))
        assertNotEquals(first, storyCardStableId(StoryCardType.CATEGORIES, "42"))
        assertNotEquals(first, storyCardStableId(StoryCardType.ALBUMS, "43"))
    }

    @Test
    fun annualDayDistance_wrapsAcrossNewYearAndHandlesLeapDay() {
        assertEquals(1L, annualDayDistance(MonthDay.of(12, 31), MonthDay.of(1, 1)))
        assertEquals(3L, annualDayDistance(MonthDay.of(12, 29), MonthDay.of(1, 1)))
        assertEquals(1L, annualDayDistance(MonthDay.of(2, 29), MonthDay.of(3, 1)))
        assertEquals(0L, annualDayDistance(MonthDay.of(6, 15), MonthDay.of(6, 15)))
    }

    @Test
    fun duration_clampsPersistedAndMalformedValues() {
        assertEquals(5_000L, storyDurationMillis("invalid"))
        assertEquals(3_000L, storyDurationMillis("-1"))
        assertEquals(3_000L, storyDurationMillis("3"))
        assertEquals(7_000L, storyDurationMillis("7"))
        assertEquals(10_000L, storyDurationMillis("99"))
    }

    @Test
    fun dismissProgress_clampsAndUsesTheSameThresholdAsTheGesture() {
        assertEquals(0f, viewerDismissProgress(-20f, height = 1_000), 0f)
        assertEquals(0.5f, viewerDismissProgress(175f, height = 1_000), 0.001f)
        assertEquals(1f, viewerDismissProgress(500f, height = 1_000), 0f)
        assertEquals(0f, viewerDismissProgress(200f, height = 0), 0f)
        assertFalse(shouldCommitViewerDismiss(119f, height = 1_000))
        assertTrue(shouldCommitViewerDismiss(120f, height = 1_000))
    }

    @Test
    fun elapsedProgress_doesNotBankPausedTimeAndCapsAtDuration() {
        assertEquals(1_000L, advanceStoryElapsed(1_000L, 5_000L, 10_000L, blocked = true))
        assertEquals(6_000L, advanceStoryElapsed(1_000L, 5_000L, 10_000L, blocked = false))
        assertEquals(10_000L, advanceStoryElapsed(9_000L, 5_000L, 10_000L, blocked = false))
        assertEquals(1_000L, advanceStoryElapsed(1_000L, -5L, 10_000L, blocked = false))
    }

    @Test
    fun moveStoryCardType_movesValidIndicesAndLeavesInvalidRequestsUntouched() {
        val order = listOf(
            StoryCardType.MEMORIES,
            StoryCardType.ALBUMS,
            StoryCardType.CATEGORIES,
        )

        assertEquals(
            listOf(
                StoryCardType.ALBUMS,
                StoryCardType.CATEGORIES,
                StoryCardType.MEMORIES,
            ),
            moveStoryCardType(order, 0, 2),
        )
        assertSame(order, moveStoryCardType(order, -1, 2))
        assertSame(order, moveStoryCardType(order, 1, 1))
    }

    private fun storyCard(id: Long, type: StoryCardType) = StoryCard(
        id = id,
        type = type,
        title = type.name,
    )
}
