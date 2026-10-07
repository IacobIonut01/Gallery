/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class StartupPrefillTest {

    @Test
    fun `cache hit reuses the ready signal`() = runTest(StandardTestDispatcher()) {
        var awaited = false
        boundedPrefill(
            route = "a", selectedRoute = "a",
            cached = { true },
            awaitReady = { awaited = true }
        )
        assertTrue(awaited)
    }

    @Test
    fun `cache miss never invokes the full-load callback`() = runTest(StandardTestDispatcher()) {
        boundedPrefill(
            route = "a", selectedRoute = "a",
            cached = { false },
            awaitReady = { fail("awaitReady must not run on a cache miss") }
        )
    }

    @Test
    fun `non-selected routes skip the cache probe entirely`() = runTest(StandardTestDispatcher()) {
        var probed = false
        boundedPrefill(
            route = "a", selectedRoute = "b",
            cached = { probed = true; true },
            awaitReady = { fail("awaitReady must not run for another route") }
        )
        assertFalse(probed)
    }

    @Test
    fun `prefill returns at the timeout cap instead of waiting forever`() =
        runTest(StandardTestDispatcher()) {
            var started = false
            boundedPrefill(
                route = "a", selectedRoute = "a",
                timeoutMillis = PREFILL_TIMEOUT_MS,
                cached = { true },
                awaitReady = {
                    started = true
                    CompletableDeferred<Unit>().await()
                }
            )
            assertTrue(started)
            assertEquals(PREFILL_TIMEOUT_MS, currentTime)
        }

    @Test
    fun `external cancellation propagates through the timeout`() =
        runTest(StandardTestDispatcher()) {
            val job = launch {
                boundedPrefill(
                    route = "a", selectedRoute = "a",
                    cached = { true },
                    awaitReady = { CompletableDeferred<Unit>().await() }
                )
            }
            runCurrent()
            job.cancel()
            job.join()
            assertTrue(job.isCancelled)
        }

    @Test
    fun `cancellation inside the cache probe propagates`() = runTest(StandardTestDispatcher()) {
        val job = launch {
            boundedPrefill(
                route = "a", selectedRoute = "a",
                cached = { throw CancellationException("probe cancelled") },
                awaitReady = { fail("awaitReady must not run") }
            )
        }
        runCurrent()
        job.join()
        assertTrue(job.isCancelled)
    }

    @Test
    fun `cache probe failure is contained with no ready wait`() = runTest(StandardTestDispatcher()) {
        val errors = mutableListOf<Throwable>()
        boundedPrefill(
            route = "a", selectedRoute = "a",
            onError = { errors += it },
            cached = { throw IllegalStateException("decrypt failed") },
            awaitReady = { fail("awaitReady must not run after a probe failure") }
        )
        assertEquals(1, errors.size)
        assertTrue(errors[0] is IllegalStateException)
        assertEquals("decrypt failed", errors[0].message)
    }
}
