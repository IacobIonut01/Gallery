/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import com.dot.gallery.core.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.domain.uri.query


@OptIn(ExperimentalCoroutinesApi::class)
class StartupLoadFlowTest {

    private val stamp = StartupCacheStamp("v1", PERMISSION_MASK_FULL_MEDIA)

    private fun neverLive(): Flow<List<String>> = flow {
        CompletableDeferred<Unit>().await()
    }

    private fun harness(
        cached: List<String>? = listOf("cached"),
        bounded: List<String>? = listOf("bounded"),
        hasBoundedSource: Boolean = true,
        liveSource: Flow<List<String>> = neverLive(),
        gateReleased: Boolean = true,
        liveError: Throwable? = null,
        readError: Throwable? = null,
        boundedError: Throwable? = null,
        stampError: Throwable? = null,
        writeError: Throwable? = null,
        errorMessage: String = "Failed to load media"
    ): Harness {
        val events = mutableListOf<String>()
        val writes = mutableListOf<Pair<StartupCacheStamp?, List<String>>>()
        val liveErrors = mutableListOf<Throwable>()
        val live = if (liveError != null) flow<List<String>> { throw liveError } else liveSource
        val flow = startupLoadFlow(
            readCache = {
                events += "read"
                readError?.let { throw it }
                cached
            },
            boundedSource = if (!hasBoundedSource) null else {
                {
                    events += "bounded"
                    boundedError?.let { throw it }
                    bounded!!
                }
            },
            awaitFirstContent = {
                events += "gate"
                if (!gateReleased) CompletableDeferred<Unit>().await()
            },
            currentStamp = {
                events += "stamp"
                stampError?.let { throw it }
                stamp
            },
            liveSource = flow {
                events += "live"
                live.collect { emit(it) }
            },
            writeCache = { s, items ->
                events += "write(${items.size})"
                writeError?.let { throw it }
                writes += s to items
            },
            onLiveError = { liveErrors += it },
            errorMessage = errorMessage
        )
        return Harness(flow, events, writes, liveErrors)
    }

    private class Harness(
        val flow: Flow<Resource<List<String>>>,
        val events: MutableList<String>,
        val writes: MutableList<Pair<StartupCacheStamp?, List<String>>>,
        val liveErrors: MutableList<Throwable>
    )

    private fun TestScope.collectInto(
        h: Harness,
        results: MutableList<Resource<List<String>>> = mutableListOf(),
        onEach: (Resource<List<String>>) -> Unit = {}
    ): Pair<MutableList<Resource<List<String>>>, Job> =
        results to launch {
            h.flow.collect {
                results += it
                onEach(it)
            }
        }

    @Test
    fun `cached content precedes the suspended live source`() = runTest(StandardTestDispatcher()) {
        val h = harness()
        val (results, job) = collectInto(h)
        advanceUntilIdle()
        assertEquals(listOf("cached"), (results[0] as Resource.Success).data)
        assertEquals(listOf("read", "gate", "stamp", "live"), h.events)
        assertEquals(1, results.size)
        assertTrue(h.writes.isEmpty())
        job.cancel()
    }

    @Test
    fun `cache miss emits the bounded source before the gate`() = runTest(StandardTestDispatcher()) {
        val h = harness(cached = null)
        val (results, job) = collectInto(h)
        advanceUntilIdle()
        assertEquals(listOf("bounded"), (results[0] as Resource.Success).data)
        assertEquals(listOf("read", "bounded", "gate", "stamp", "live"), h.events)
        job.cancel()
    }

    @Test
    fun `known-empty cache emits empty and never runs the bounded query`() =
        runTest(StandardTestDispatcher()) {
            val h = harness(cached = emptyList())
            val (results, job) = collectInto(h)
            advanceUntilIdle()
            assertEquals(emptyList<String>(), (results[0] as Resource.Success).data)
            assertTrue("bounded" !in h.events)
            job.cancel()
        }

    @Test
    fun `full live list is emitted untruncated and then persisted`() =
        runTest(StandardTestDispatcher()) {
            val full = (1..400).map { "item-$it" }
            val h = harness(liveSource = flow { emit(full) })
            val (results, job) = collectInto(h) { resource ->
                (resource as? Resource.Success)?.data?.let { h.events += "collected(${it.size})" }
            }
            advanceUntilIdle()
            assertEquals(listOf("cached"), (results[0] as Resource.Success).data)
            assertEquals(400, (results[1] as Resource.Success).data!!.size)
            assertEquals(listOf(stamp to full), h.writes)
            assertEquals(
                listOf(
                    "read", "collected(1)", "gate", "stamp", "live",
                    "collected(400)", "write(400)"
                ),
                h.events
            )
            job.cancel()
        }

    @Test
    fun `bounded batch is never written to cache`() = runTest(StandardTestDispatcher()) {
        val h = harness(
            cached = null,
            liveSource = flow { emit(listOf("live-full")) }
        )
        val (results, job) = collectInto(h)
        advanceUntilIdle()
        assertEquals(2, results.size)
        assertEquals(listOf(stamp to listOf("live-full")), h.writes)
        job.cancel()
    }

    @Test
    fun `initial cached emission is marked partial`() = runTest(StandardTestDispatcher()) {
        val h = harness()
        val (results, job) = collectInto(h)
        advanceUntilIdle()
        assertTrue((results[0] as Resource.Success).isPartial)
        job.cancel()
    }

    @Test
    fun `initial bounded emission is marked partial`() = runTest(StandardTestDispatcher()) {
        val h = harness(cached = null)
        val (results, job) = collectInto(h)
        advanceUntilIdle()
        assertTrue((results[0] as Resource.Success).isPartial)
        job.cancel()
    }

    @Test
    fun `live emissions are complete rather than partial`() =
        runTest(StandardTestDispatcher()) {
            val h = harness(liveSource = flow { emit(listOf("live-full")) })
            val (results, job) = collectInto(h)
            advanceUntilIdle()
            assertTrue((results[0] as Resource.Success).isPartial)
            assertTrue(!(results[1] as Resource.Success).isPartial)
            job.cancel()
        }

    @Test
    fun `live failure after cached content preserves the emission`() =
        runTest(StandardTestDispatcher()) {
            val boom = IllegalStateException("mediastore blew up")
            val h = harness(liveError = boom)
            val (results, job) = collectInto(h)
            advanceUntilIdle()
            assertEquals(1, results.size)
            assertEquals(listOf("cached"), (results[0] as Resource.Success).data)
            assertEquals(listOf(boom), h.liveErrors)
            job.cancel()
        }

    @Test
    fun `cache read failure falls back to the bounded source`() =
        runTest(StandardTestDispatcher()) {
            val boom = IllegalStateException("decrypt failed")
            val h = harness(readError = boom)
            val (results, job) = collectInto(h)
            advanceUntilIdle()
            assertEquals(listOf("bounded"), (results[0] as Resource.Success).data)
            assertEquals(listOf("read", "bounded", "gate", "stamp", "live"), h.events)
            assertEquals(listOf(boom), h.liveErrors)
            job.cancel()
        }

    @Test
    fun `cache read failure without a bounded source falls straight to live`() =
        runTest(StandardTestDispatcher()) {
            val boom = IllegalStateException("decrypt failed")
            val h = harness(
                readError = boom,
                hasBoundedSource = false,
                liveSource = flow { emit(listOf("live-full")) }
            )
            val (results, job) = collectInto(h)
            advanceUntilIdle()
            assertEquals(1, results.size)
            assertEquals(listOf("live-full"), (results[0] as Resource.Success).data)
            assertEquals(listOf("read", "stamp", "live", "write(1)"), h.events)
            assertEquals(listOf(boom), h.liveErrors)
            job.cancel()
        }

    @Test
    fun `bounded source failure surfaces Resource Error`() = runTest(StandardTestDispatcher()) {
        val boom = IllegalStateException("mediastore query failed")
        val h = harness(cached = null, boundedError = boom)
        val (results, job) = collectInto(h)
        advanceUntilIdle()
        assertEquals(1, results.size)
        assertTrue(results[0] is Resource.Error)
        assertEquals("Failed to load media", results[0].message)
        assertEquals(listOf(boom), h.liveErrors)
        job.cancel()
    }

    @Test
    fun `live failure before any emission surfaces Resource Error`() =
        runTest(StandardTestDispatcher()) {
            val boom = IllegalStateException("live source failed")
            val h = harness(
                cached = null,
                hasBoundedSource = false,
                liveError = boom,
                errorMessage = "Failed to load albums"
            )
            val (results, job) = collectInto(h)
            advanceUntilIdle()
            assertEquals(1, results.size)
            assertTrue(results[0] is Resource.Error)
            assertEquals("Failed to load albums", results[0].message)
            assertEquals(listOf(boom), h.liveErrors)
            job.cancel()
        }

    @Test
    fun `downstream failure propagates without a second emission`() =
        runTest(StandardTestDispatcher()) {
            val h = harness(liveSource = flow { emit(listOf("live-full")) })
            val downstream = IllegalStateException("collector blew up")
            val results = mutableListOf<Resource<List<String>>>()
            var caught: Throwable? = null
            val job = launch {
                try {
                    h.flow.collect {
                        results += it
                        if (results.size == 2) throw downstream
                    }
                } catch (e: IllegalStateException) {
                    caught = e
                }
            }
            advanceUntilIdle()
            job.join()
            assertTrue(caught is IllegalStateException)
            assertEquals("collector blew up", caught?.message)
            assertEquals(2, results.size)
            assertTrue(results.none { it is Resource.Error })
            assertTrue("write(1)" !in h.events)
        }

    @Test
    fun `writer failure preserves subsequent live emissions`() =
        runTest(StandardTestDispatcher()) {
            val boom = IllegalStateException("datastore write failed")
            val h = harness(
                writeError = boom,
                liveSource = flow {
                    emit(listOf("live-1"))
                    emit(listOf("live-2"))
                }
            )
            val (results, job) = collectInto(h)
            advanceUntilIdle()
            job.join()
            assertEquals(3, results.size)
            assertEquals(listOf("live-1"), (results[1] as Resource.Success).data)
            assertEquals(listOf("live-2"), (results[2] as Resource.Success).data)
            assertTrue(h.writes.isEmpty())
            assertEquals(listOf(boom, boom), h.liveErrors)
        }

    @Test
    fun `cancellation inside the cache read propagates`() = runTest(StandardTestDispatcher()) {
        val h = harness(readError = CancellationException("read cancelled"))
        val job = launch { h.flow.collect { } }
        advanceUntilIdle()
        job.join()
        assertTrue(job.isCancelled)
    }

    @Test
    fun `cancellation inside the bounded source propagates`() = runTest(StandardTestDispatcher()) {
        val h = harness(cached = null, boundedError = CancellationException("bounded cancelled"))
        val job = launch { h.flow.collect { } }
        advanceUntilIdle()
        job.join()
        assertTrue(job.isCancelled)
        assertTrue(h.liveErrors.isEmpty())
    }

    @Test
    fun `cancellation inside the stamp read propagates`() = runTest(StandardTestDispatcher()) {
        val h = harness(
            cached = null,
            stampError = CancellationException("stamp cancelled")
        )
        val job = launch { h.flow.collect { } }
        advanceUntilIdle()
        job.join()
        assertTrue(job.isCancelled)
    }

    @Test
    fun `cancellation while awaiting the gate propagates`() = runTest(StandardTestDispatcher()) {
        val h = harness(gateReleased = false)
        val results = mutableListOf<Resource<List<String>>>()
        val job = launch { h.flow.collect { results += it } }
        advanceUntilIdle()
        assertEquals(listOf("cached"), (results[0] as Resource.Success).data)
        assertEquals(listOf("read", "gate"), h.events)
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
    }

    @Test
    fun `cancellation inside the live source propagates`() = runTest(StandardTestDispatcher()) {
        val h = harness(
            liveSource = flow { throw CancellationException("collector gone") }
        )
        val job = launch {
            h.flow.collect { }
        }
        advanceUntilIdle()
        job.join()
        assertTrue(job.isCancelled)
        assertTrue(h.writes.isEmpty())
    }

    @Test
    fun `cancellation inside the cache write propagates`() = runTest(StandardTestDispatcher()) {
        val h = harness(
            writeError = CancellationException("write cancelled"),
            liveSource = flow { emit(listOf("live-1")) }
        )
        val job = launch { h.flow.collect { } }
        advanceUntilIdle()
        job.join()
        assertTrue(job.isCancelled)
        assertTrue(h.liveErrors.isEmpty())
    }
}
