/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.sync.runWorkerPool
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class CloudUploadWorkerPoolTest {

    @Test
    fun processesEveryItemWithBoundedConcurrency() = runBlocking {
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val processed = ConcurrentHashMap.newKeySet<Int>()

        runWorkerPool((0 until 20).toList(), maxConcurrency = 3) { item ->
            val current = active.incrementAndGet()
            peak.updateAndGet { previous -> maxOf(previous, current) }
            delay(10)
            processed += item
            active.decrementAndGet()
        }

        assertEquals((0 until 20).toSet(), processed)
        assertEquals(3, peak.get())
    }

    @Test
    fun doesNotCreateMoreWorkersThanItems() = runBlocking {
        val active = AtomicInteger()
        val peak = AtomicInteger()

        runWorkerPool(listOf("one", "two"), maxConcurrency = 3) {
            val current = active.incrementAndGet()
            peak.updateAndGet { previous -> maxOf(previous, current) }
            delay(10)
            active.decrementAndGet()
        }

        assertTrue(peak.get() <= 2)
    }
}
