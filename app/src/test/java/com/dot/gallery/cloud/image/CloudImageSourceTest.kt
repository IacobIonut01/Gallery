/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.image

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudImageSourceTest {

    @Test
    fun cloudSubsamplingUsesFormatSpecificDecoder() {
        assertEquals(
            CloudSubsamplingMode.JXL,
            resolveCloudSubsamplingMode(
                isJxl = true,
                hasCustomRegionDecoder = false,
                isHeif = false,
                isSpecialFormat = false,
                isAnimated = true,
                isAnimatedRaster = false
            )
        )
        assertEquals(
            CloudSubsamplingMode.CUSTOM,
            resolveCloudSubsamplingMode(
                isJxl = false,
                hasCustomRegionDecoder = true,
                isHeif = false,
                isSpecialFormat = false,
                isAnimated = false,
                isAnimatedRaster = false
            )
        )
        assertEquals(
            CloudSubsamplingMode.HEIF,
            resolveCloudSubsamplingMode(
                isJxl = false,
                hasCustomRegionDecoder = false,
                isHeif = true,
                isSpecialFormat = false,
                isAnimated = false,
                isAnimatedRaster = false
            )
        )
    }

    @Test
    fun cloudSubsamplingSkipsUnsupportedAnimations() {
        assertEquals(
            CloudSubsamplingMode.NONE,
            resolveCloudSubsamplingMode(
                isJxl = false,
                hasCustomRegionDecoder = false,
                isHeif = false,
                isSpecialFormat = false,
                isAnimated = true,
                isAnimatedRaster = false
            )
        )
        assertEquals(
            CloudSubsamplingMode.NONE,
            resolveCloudSubsamplingMode(
                isJxl = false,
                hasCustomRegionDecoder = false,
                isHeif = false,
                isSpecialFormat = false,
                isAnimated = false,
                isAnimatedRaster = true
            )
        )
        assertEquals(
            CloudSubsamplingMode.NONE,
            resolveCloudSubsamplingMode(
                isJxl = false,
                hasCustomRegionDecoder = false,
                isHeif = false,
                isSpecialFormat = true,
                isAnimated = false,
                isAnimatedRaster = false
            )
        )
    }

    @Test
    fun originalLoadsOnlyForSelectedSupportedMedia() {
        assertTrue(
            shouldLoadCloudOriginal(
                isSelected = true,
                subsamplingMode = CloudSubsamplingMode.PLATFORM
            )
        )
        assertFalse(
            shouldLoadCloudOriginal(
                isSelected = false,
                subsamplingMode = CloudSubsamplingMode.PLATFORM
            )
        )
        assertFalse(
            shouldLoadCloudOriginal(
                isSelected = true,
                subsamplingMode = CloudSubsamplingMode.NONE
            )
        )
    }

    @Test
    fun concurrentOriginalWritesAreSingleFlight() = runBlocking {
        val directory = Files.createTempDirectory("cloud-original-test").toFile()
        val target = directory.resolve("original.img")
        val payload = ByteArray(32 * 1024) { (it % 251).toByte() }
        val firstWriterStarted = CompletableDeferred<Unit>()
        val releaseWriter = CompletableDeferred<Unit>()
        val temporaryPaths = ConcurrentHashMap.newKeySet<String>()
        val writerCount = AtomicInteger()

        try {
            List(2) {
                async(Dispatchers.IO) {
                    storeCloudOriginal(target) { temporaryFile ->
                        writerCount.incrementAndGet()
                        temporaryPaths += temporaryFile.absolutePath
                        firstWriterStarted.complete(Unit)
                        releaseWriter.await()
                        temporaryFile.writeBytes(payload)
                    }
                }
            }.also { requests ->
                firstWriterStarted.await()
                releaseWriter.complete(Unit)
                requests.awaitAll()
            }

            assertEquals(1, writerCount.get())
            assertEquals(1, temporaryPaths.size)
            assertArrayEquals(payload, target.readBytes())
            assertFalse(directory.listFiles().orEmpty().any { it.name.endsWith(".tmp") })
        } finally {
            releaseWriter.complete(Unit)
            directory.deleteRecursively()
        }
    }
}
