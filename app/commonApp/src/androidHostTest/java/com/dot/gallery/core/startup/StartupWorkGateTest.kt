/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class StartupWorkGateTest {

    private val gate = StartupWorkGate()

    @Test
    fun `awaitFirstContent waits before content is drawn`() = runTest(StandardTestDispatcher()) {
        var released = false
        val job = launch {
            gate.awaitFirstContent()
            released = true
        }
        advanceTimeBy(1_000L)
        assertFalse(released)
        gate.onContentDrawn()
        advanceUntilIdle()
        assertTrue(released)
        job.join()
    }

    @Test
    fun `onContentDrawn releases all waiters once`() = runTest(StandardTestDispatcher()) {
        var released = 0
        repeat(3) {
            launch {
                gate.awaitFirstContent()
                released++
            }
        }
        advanceTimeBy(500L)
        gate.onContentDrawn()
        advanceUntilIdle()
        assertEquals(3, released)
    }

    @Test
    fun `release is idempotent for late waiters`() = runTest(StandardTestDispatcher()) {
        gate.onContentDrawn()
        gate.onContentDrawn()
        var released = false
        launch {
            gate.awaitFirstContent()
            released = true
        }
        advanceUntilIdle()
        assertTrue(released)
    }

    @Test
    fun `fallback releases waiters without content for headless work`() =
        runTest(StandardTestDispatcher()) {
            var releasedAt = -1L
            launch {
                gate.awaitFirstContent()
                releasedAt = currentTime
            }
            advanceTimeBy(1_499L)
            assertEquals(-1L, releasedAt)
            advanceTimeBy(2L)
            assertTrue(releasedAt >= 1_500L)
        }

    @Test
    fun `cancellation propagates to waiters`() = runTest(StandardTestDispatcher()) {
        val waiter = async {
            gate.awaitFirstContent()
            "done"
        }
        advanceTimeBy(100L)
        waiter.cancel()
        try {
            waiter.await()
            fail("expected CancellationException")
        } catch (_: CancellationException) {
        }
    }
}
