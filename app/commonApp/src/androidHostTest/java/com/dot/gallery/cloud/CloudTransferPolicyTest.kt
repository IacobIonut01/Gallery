/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.core.CloudAlbumIdentity
import com.dot.gallery.cloud.core.CloudUri
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.isUnsupportedSameAccountCloudMove
import com.dot.gallery.cloud.core.shouldDeleteCloudSourceAfterTransfer
import com.dot.gallery.cloud.sync.CloudAlbumCopyItemState
import com.dot.gallery.cloud.sync.CloudAlbumTransferMode
import com.dot.gallery.cloud.sync.isTransferFailure
import com.dot.gallery.cloud.sync.shouldBeginCloudMoveCleanup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class CloudTransferPolicyTest {

    @Test
    fun pathProviderRejectsItsCurrentFolderButAllowsAnotherFolder() {
        val source = cloudUri(ProviderType.SMB, 11L, "Camera/photo.jpg")

        assertTrue(
            isUnsupportedSameAccountCloudMove(
                source,
                CloudAlbumIdentity(ProviderType.SMB, 11L, "Camera"),
                "photo.jpg"
            )
        )
        assertFalse(
            isUnsupportedSameAccountCloudMove(
                source,
                CloudAlbumIdentity(ProviderType.SMB, 11L, "Archive"),
                "photo.jpg"
            )
        )
    }

    @Test
    fun immichMoveWithinOneAccountIsRejectedButCrossAccountIsAllowed() {
        val source = cloudUri(ProviderType.IMMICH, 11L, "asset-1")

        assertTrue(
            isUnsupportedSameAccountCloudMove(
                source,
                CloudAlbumIdentity(ProviderType.IMMICH, 11L, "album-2"),
                "photo.jpg"
            )
        )
        assertFalse(
            isUnsupportedSameAccountCloudMove(
                source,
                CloudAlbumIdentity(ProviderType.IMMICH, 22L, "album-2"),
                "photo.jpg"
            )
        )
    }

    @Test
    fun sourceDeletionRequiresADistinctVerifiedDestinationAsset() {
        val source = cloudUri(ProviderType.SMB, 11L, "Camera/photo.jpg")
        val sameAccount = CloudAlbumIdentity(ProviderType.SMB, 11L, "Camera")

        assertFalse(shouldDeleteCloudSourceAfterTransfer(source, sameAccount, null))
        assertFalse(
            shouldDeleteCloudSourceAfterTransfer(
                source,
                sameAccount,
                "Camera/photo.jpg"
            )
        )
        assertTrue(
            shouldDeleteCloudSourceAfterTransfer(
                source,
                CloudAlbumIdentity(ProviderType.SMB, 11L, "Archive"),
                "Archive/photo.jpg"
            )
        )
        assertTrue(
            shouldDeleteCloudSourceAfterTransfer(
                source,
                CloudAlbumIdentity(ProviderType.SMB, 22L, "Camera"),
                "Camera/photo.jpg"
            )
        )
    }

    @Test
    fun moveCleanupWaitsForEveryDestinationCopyAndTreatsRetainedSourcesAsFailure() {
        assertFalse(
            shouldBeginCloudMoveCleanup(
                CloudAlbumTransferMode.COPY,
                listOf(CloudAlbumCopyItemState.COPIED)
            )
        )
        assertFalse(
            shouldBeginCloudMoveCleanup(
                CloudAlbumTransferMode.MOVE,
                listOf(CloudAlbumCopyItemState.COPIED, CloudAlbumCopyItemState.FAILED)
            )
        )
        assertTrue(
            shouldBeginCloudMoveCleanup(
                CloudAlbumTransferMode.MOVE,
                listOf(CloudAlbumCopyItemState.COPIED, CloudAlbumCopyItemState.ALREADY_PRESENT)
            )
        )
        assertTrue(CloudAlbumCopyItemState.SOURCE_DELETE_PENDING.isTransferFailure())
        assertTrue(CloudAlbumCopyItemState.SOURCE_RETAINED.isTransferFailure())
        assertFalse(CloudAlbumCopyItemState.MOVED.isTransferFailure())
    }

    private fun cloudUri(type: ProviderType, configId: Long, remoteId: String) = CloudUri(
        providerType = type,
        remoteId = remoteId,
        size = "original",
        typeParam = null,
        fileId = null,
        configId = configId
    )
}
