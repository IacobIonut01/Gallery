/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.feature.search.rankedRemoteHits


class RemoteSearchRankingTest {

    @Test
    fun emptyInputProducesNoHits() {
        assertTrue(rankedRemoteHits(emptyList<String>()).isEmpty())
    }

    @Test
    fun singleHitGetsTopRemoteScore() {
        val hits = rankedRemoteHits(listOf("a"))
        assertEquals(1, hits.size)
        assertEquals(0.95f, hits[0].first)
        assertEquals("a", hits[0].second)
    }

    @Test
    fun scoresDecayFromTopToFloorPreservingOrder() {
        val remote = listOf("a", "b", "c", "d", "e")
        val hits = rankedRemoteHits(remote)

        assertEquals(remote, hits.map { it.second })
        assertEquals(0.95f, hits.first().first)
        assertEquals(0.5f, hits.last().first, 1e-6f)
        hits.zipWithNext().forEach { (a, b) -> assertTrue(a.first > b.first) }
    }
}
