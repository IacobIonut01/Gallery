/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import androidx.work.WorkInfo
import com.dot.gallery.cloud.sync.CloudUploadWorker
import com.dot.gallery.cloud.sync.backupDestinationConfigIds
import com.dot.gallery.cloud.sync.isActiveBackupWork
import com.dot.gallery.cloud.sync.isBackupRevisionCached
import com.dot.gallery.cloud.sync.mapWorkerPool
import com.dot.gallery.cloud.ui.backup.BackupMatchEvidence
import com.dot.gallery.cloud.ui.backup.backupMatchEvidence
import com.dot.gallery.cloud.ui.backup.visibleBackupProgressItems
import com.dot.gallery.cloud.sync.runWorkerPool
import com.dot.gallery.cloud.sync.shouldDeferChecksumCheck
import com.dot.gallery.cloud.immich.data.dto.ImmichAssetDto
import com.dot.gallery.cloud.immich.data.dto.ImmichBulkCheckResultItemDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class CloudUploadWorkerPoolTest {

    @Test
    fun dormantPeriodicWorkIsNotReportedAsAnActiveUpload() {
        assertTrue(
            !isActiveBackupWork(
                WorkInfo.State.ENQUEUED,
                setOf(CloudUploadWorker.TAG_BACKUP, CloudUploadWorker.TAG_PERIODIC_BACKUP)
            )
        )
        assertTrue(
            isActiveBackupWork(
                WorkInfo.State.ENQUEUED,
                setOf(CloudUploadWorker.TAG_BACKUP, CloudUploadWorker.TAG_MANUAL_BACKUP)
            )
        )
        assertTrue(
            isActiveBackupWork(
                WorkInfo.State.RUNNING,
                setOf(CloudUploadWorker.TAG_BACKUP, CloudUploadWorker.TAG_PERIODIC_BACKUP)
            )
        )
    }

    @Test
    fun checksumVerificationUsesCheckedItemsInsteadOfStayingAtZero() {
        assertEquals(
            40,
            visibleBackupProgressItems(
                phase = CloudUploadWorker.PHASE_VERIFYING,
                checkedItems = 40,
                completedItems = 0,
                failedItems = 0
            )
        )
        assertEquals(
            7,
            visibleBackupProgressItems(
                phase = CloudUploadWorker.PHASE_UPLOADING,
                checkedItems = 40,
                completedItems = 5,
                failedItems = 2
            )
        )
    }

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

    @Test
    fun deleteLocalCoverageIncludesEveryConfiguredDestination() {
        val destinations = backupDestinationConfigIds(
            albumId = 10L,
            albumIdsByConfig = mapOf(
                1L to setOf(10L, 20L),
                2L to setOf(10L),
                3L to setOf(30L)
            )
        )

        assertEquals(setOf(1L, 2L), destinations)
    }

    @Test
    fun immichAssetRevisionUsesModifiedTimeAndDeviceAssetId() {
        val entity = ImmichAssetDto(
            id = "remote-id",
            deviceAssetId = "42",
            originalFileName = "photo.jpg",
            originalMimeType = "image/jpeg",
            fileCreatedAt = "2026-01-01T00:00:00Z",
            fileModifiedAt = "2026-01-02T03:04:05Z"
        ).toCloudMediaEntity(serverConfigId = 7L, baseUrl = "https://immich.example")

        assertEquals("42", entity.fileId)
        assertEquals(1767323045000L, entity.lastSyncedAt)
    }

    @Test
    fun immichPresenceRequiresNonTrashedDuplicate() {
        val duplicate = ImmichBulkCheckResultItemDto(
            action = "reject",
            assetId = "asset-id",
            reason = "duplicate",
            isTrashed = false
        )

        assertTrue(duplicate.isSafeDuplicate())
        assertTrue(!duplicate.copy(isTrashed = true).isSafeDuplicate())
        assertTrue(!duplicate.copy(reason = "unsupported-format").isSafeDuplicate())
        assertTrue(!duplicate.copy(assetId = null).isSafeDuplicate())
    }

    @Test
    fun remoteRevisionCanBeDetectedButMustNotBeUsedForImmichChecksumBypass() {
        val remoteRevisions = setOf("42|photo.jpg|image/jpeg|1234|100")

        assertTrue(
            isBackupRevisionCached(
                uri = "content://media/42",
                mediaId = 42L,
                label = "photo.jpg",
                mimeType = "image/jpeg",
                size = 1234L,
                timestamp = 100L,
                localRevisions = emptySet(),
                remoteRevisions = remoteRevisions
            )
        )
        assertTrue(
            !isBackupRevisionCached(
                uri = "content://media/42",
                mediaId = 42L,
                label = "photo.jpg",
                mimeType = "image/jpeg",
                size = 1234L,
                timestamp = 101L,
                localRevisions = emptySet(),
                remoteRevisions = remoteRevisions
            )
        )
    }

    @Test
    fun sameAppRemoteRevisionRemainsAnAssumptionUntilContentIsChecked() {
        assertEquals(
            BackupMatchEvidence.ASSUMED_REVISION,
            backupMatchEvidence(
                uri = "content://media/42",
                mediaId = 42L,
                label = "photo.jpg",
                mimeType = "image/jpeg",
                size = 1234L,
                timestamp = 100L,
                cachedNames = setOf("photo.jpg"),
                localRevisions = emptySet(),
                remoteRevisions = setOf("42|photo.jpg|image/jpeg|1234|100")
            )
        )
        assertEquals(
            BackupMatchEvidence.ASSUMED_FILENAME,
            backupMatchEvidence(
                uri = "content://media/99",
                mediaId = 99L,
                label = "photo.jpg",
                mimeType = "image/jpeg",
                size = 1234L,
                timestamp = 100L,
                cachedNames = setOf("photo.jpg"),
                localRevisions = emptySet(),
                remoteRevisions = setOf("42|photo.jpg|image/jpeg|1234|100")
            )
        )
    }

    @Test
    fun smallImmichQueueUsesFastStartChecksumPath() {
        assertTrue(shouldDeferChecksumCheck(itemCount = 6, maxConcurrentUploads = 3))
        assertTrue(!shouldDeferChecksumCheck(itemCount = 7, maxConcurrentUploads = 3))
    }

    @Test
    fun mapsConcurrentlyWhilePreservingInputOrder() = runBlocking {
        val active = AtomicInteger()
        val peak = AtomicInteger()

        val mapped = mapWorkerPool((0 until 6).toList(), maxConcurrency = 3) { item ->
            val current = active.incrementAndGet()
            peak.updateAndGet { previous -> maxOf(previous, current) }
            delay((6 - item).toLong())
            active.decrementAndGet()
            item * 2
        }

        assertEquals(listOf(0, 2, 4, 6, 8, 10), mapped)
        assertEquals(3, peak.get())
    }
}
