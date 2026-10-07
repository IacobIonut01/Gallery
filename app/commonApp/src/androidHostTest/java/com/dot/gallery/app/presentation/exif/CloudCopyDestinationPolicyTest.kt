/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.exif

import com.dot.gallery.feature.exif.ui.resolveTransferOpenTarget
import com.dot.gallery.feature.exif.ui.cloudCopyDestinationStatus
import com.dot.gallery.cloud.core.CloudAlbumIdentity
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.remoteAlbumFilePath
import com.dot.gallery.cloud.core.capabilities.remoteCopyFileName
import com.dot.gallery.cloud.core.cloudAlbumId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.feature.exif.CloudCopyUiState
import com.dot.gallery.feature.exif.ui.CloudCopyDestinationStatus
import com.dot.gallery.feature.exif.ui.TransferOpenTarget
import com.dot.gallery.feature.exif.ui.TransferOpenTargetType


class CloudCopyDestinationPolicyTest {

    private val identity = CloudAlbumIdentity(
        providerType = ProviderType.SMB,
        serverConfigId = 42L,
        remoteId = "Photos/Family"
    )
    private val albumId = cloudAlbumId(
        identity.providerType,
        identity.serverConfigId,
        identity.remoteId
    )

    @Test
    fun writableExactAccountEnablesCloudDestination() {
        assertEquals(
            CloudCopyDestinationStatus.READY,
            cloudCopyDestinationStatus(
                albumId = albumId,
                identity = identity,
                sourcesSupported = true,
                accountAvailable = true,
                supportsAlbumWrite = true,
                readOnly = false,
                offline = false
            )
        )
    }

    @Test
    fun malformedOrLegacyIdentityNeverRoutesByHashedIdAlone() {
        assertEquals(
            CloudCopyDestinationStatus.MISSING_IDENTITY,
            cloudCopyDestinationStatus(
                albumId = albumId,
                identity = null,
                sourcesSupported = true,
                accountAvailable = true,
                supportsAlbumWrite = true,
                readOnly = false,
                offline = false
            )
        )
        assertEquals(
            CloudCopyDestinationStatus.MISSING_IDENTITY,
            cloudCopyDestinationStatus(
                albumId = albumId,
                identity = identity.copy(remoteId = "Photos/Other"),
                sourcesSupported = true,
                accountAvailable = true,
                supportsAlbumWrite = true,
                readOnly = false,
                offline = false
            )
        )
    }

    @Test
    fun keepBothNamesPreserveExtensionAndRemoteAlbumPath() {
        assertEquals("photo (1).jpg", remoteCopyFileName("photo.jpg", 1))
        assertEquals("archive (2)", remoteCopyFileName("archive", 2))
        assertEquals(
            "Photos/Family/photo (1).jpg",
            remoteAlbumFilePath("/Photos/Family/", "photo (1).jpg")
        )
    }

    @Test
    fun completionOpensSingleResultButUsesAlbumForBulkTransfers() {
        assertEquals(
            TransferOpenTarget(
                type = TransferOpenTargetType.MEDIA,
                albumId = -42L,
                albumLabel = "Camera (SMB)",
                mediaId = -99L
            ),
            resolveTransferOpenTarget(
                itemCount = 1,
                targetMediaId = -99L,
                destinationAlbumId = -42L,
                destinationLabel = "Camera (SMB)"
            )
        )
        assertEquals(
            TransferOpenTarget(
                type = TransferOpenTargetType.ALBUM,
                albumId = -42L,
                albumLabel = "Camera (SMB)"
            ),
            resolveTransferOpenTarget(
                itemCount = 3,
                targetMediaId = null,
                destinationAlbumId = -42L,
                destinationLabel = "Camera (SMB)"
            )
        )
    }

    @Test
    fun singleTransferFallsBackToAlbumWhenTargetIdentityIsUnavailable() {
        assertEquals(
            TransferOpenTarget(
                type = TransferOpenTargetType.ALBUM,
                albumId = 42L,
                albumLabel = "Camera"
            ),
            resolveTransferOpenTarget(
                itemCount = 1,
                targetMediaId = null,
                destinationAlbumId = 42L,
                destinationLabel = "Camera"
            )
        )
    }

    @Test
    fun retryActionRequiresARetryableTerminalFailure() {
        assertFalse(
            CloudCopyUiState(
                requestId = "request",
                finished = true,
                failed = 1,
                retryable = false
            ).canRetry
        )
        assertTrue(
            CloudCopyUiState(
                requestId = "request",
                finished = true,
                failed = 1,
                retryable = true
            ).canRetry
        )
    }

    @Test
    fun policyRejectsUnsupportedSourceAccountCapabilityAndRuntimeModes() {
        fun status(
            sourcesSupported: Boolean = true,
            accountAvailable: Boolean = true,
            supportsAlbumWrite: Boolean = true,
            readOnly: Boolean = false,
            offline: Boolean = false
        ) = cloudCopyDestinationStatus(
            albumId = albumId,
            identity = identity,
            sourcesSupported = sourcesSupported,
            accountAvailable = accountAvailable,
            supportsAlbumWrite = supportsAlbumWrite,
            readOnly = readOnly,
            offline = offline
        )

        assertEquals(CloudCopyDestinationStatus.SOURCE_UNSUPPORTED, status(sourcesSupported = false))
        assertEquals(CloudCopyDestinationStatus.ACCOUNT_UNAVAILABLE, status(accountAvailable = false))
        assertEquals(CloudCopyDestinationStatus.PROVIDER_UNSUPPORTED, status(supportsAlbumWrite = false))
        assertEquals(CloudCopyDestinationStatus.READ_ONLY, status(readOnly = true))
        assertEquals(CloudCopyDestinationStatus.OFFLINE, status(offline = true))
    }
}
