package com.dot.gallery.cloud

import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.ui.backup.AccountBackupStatus
import com.dot.gallery.cloud.ui.offline.OfflineAvailabilityStatus
import com.dot.gallery.cloud.ui.offline.OfflineCoverage
import com.dot.gallery.cloud.ui.offline.OfflineDownloadWorkState
import com.dot.gallery.cloud.sync.isRetryableOfflineHttpStatus
import com.dot.gallery.cloud.ui.offline.offlineAvailabilityStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudUxTruthfulnessTest {

    @Test
    fun pinIntentIsNotReportedAsCompleteBeforeVariantsExist() {
        assertEquals(
            OfflineAvailabilityStatus.PINNED,
            offlineAvailabilityStatus(
                pinned = true,
                coverage = OfflineCoverage(downloadedVariants = 0, totalVariants = 4),
                workState = OfflineDownloadWorkState.IDLE
            )
        )
        assertEquals(
            OfflineAvailabilityStatus.QUEUED,
            offlineAvailabilityStatus(
                pinned = true,
                coverage = OfflineCoverage(downloadedVariants = 0, totalVariants = 4),
                workState = OfflineDownloadWorkState.QUEUED
            )
        )
    }

    @Test
    fun offlineCoverageDistinguishesPartialCompleteAndFailed() {
        assertEquals(
            OfflineAvailabilityStatus.PARTIAL,
            offlineAvailabilityStatus(
                pinned = true,
                coverage = OfflineCoverage(downloadedVariants = 2, totalVariants = 4),
                workState = OfflineDownloadWorkState.RUNNING
            )
        )
        assertEquals(
            OfflineAvailabilityStatus.COMPLETE,
            offlineAvailabilityStatus(
                pinned = true,
                coverage = OfflineCoverage(downloadedVariants = 4, totalVariants = 4),
                workState = OfflineDownloadWorkState.SUCCEEDED
            )
        )
        assertEquals(
            OfflineAvailabilityStatus.FAILED,
            offlineAvailabilityStatus(
                pinned = true,
                coverage = OfflineCoverage(downloadedVariants = 1, totalVariants = 4),
                workState = OfflineDownloadWorkState.FAILED
            )
        )
    }

    @Test
    fun offlineWorkerRetriesTransientHttpFailuresOnly() {
        assertEquals(true, isRetryableOfflineHttpStatus(408))
        assertEquals(true, isRetryableOfflineHttpStatus(429))
        assertEquals(true, isRetryableOfflineHttpStatus(503))
        assertEquals(false, isRetryableOfflineHttpStatus(401))
        assertEquals(false, isRetryableOfflineHttpStatus(404))
    }

    @Test
    fun filenameMatchesRemainAssumedRatherThanVerifiedBackups() {
        val status = AccountBackupStatus(
            configId = 7L,
            providerType = ProviderType.IMMICH,
            accountLabel = "Photos",
            enabledAlbumCount = 1,
            totalAssets = 10,
            verifiedCount = 3,
            assumedCount = 4,
            connectionState = ConnectionState.CONNECTED
        )

        assertEquals(3, status.backedUpCount)
        assertEquals(4, status.assumedCount)
        assertEquals(3, status.unknownCount)
        assertEquals(7, status.remainderCount)
        assertEquals(0.3f, status.progress)
    }
}
