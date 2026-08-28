/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.exif

import com.dot.gallery.feature_node.domain.model.resolveAlbumAbsolutePath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDestinationPolicyTest {

    @Test
    fun matchingMediaStoreVolumesEnableExistingAlbum() {
        assertTrue(
            isAlbumMoveDestinationEnabled(
                hasFullMediaAccess = false,
                albumVolume = "external_primary",
                albumRelativePath = "Pictures/Target/",
                sources = listOf(
                    AlbumDestinationSource("external_primary", "DCIM/Camera/"),
                    AlbumDestinationSource("external_primary", "Pictures/Source/"),
                ),
            )
        )
    }

    @Test
    fun anyDifferentMediaStoreVolumeDisablesBatchWithoutFullAccess() {
        assertFalse(
            isAlbumMoveDestinationEnabled(
                hasFullMediaAccess = false,
                albumVolume = "external_primary",
                albumRelativePath = "Pictures/Target/",
                sources = listOf(
                    AlbumDestinationSource("external_primary", "DCIM/Camera/"),
                    AlbumDestinationSource("71f8-2c0a", "Pictures/Source/"),
                ),
            )
        )
    }

    @Test
    fun androidMediaDestinationIsDisabledWithoutFullAccess() {
        val regularSource = listOf(AlbumDestinationSource("external_primary", "DCIM/Camera/"))

        assertFalse(
            isAlbumMoveDestinationEnabled(
                false,
                "external_primary",
                "Android/media/example.app/Pictures/",
                regularSource,
            )
        )
        assertFalse(
            isAlbumCopyDestinationEnabled(
                hasFullMediaAccess = false,
                albumRelativePath = "Android/media/example.app/Pictures/",
            )
        )
    }

    @Test
    fun fullAccessAllowsCrossVolumeButNeverCloudDestination() {
        val sources = listOf(AlbumDestinationSource("external_primary", "DCIM/Camera/"))

        assertTrue(isAlbumMoveDestinationEnabled(true, "71f8-2c0a", "Pictures/Target/", sources))
        assertFalse(
            isAlbumMoveDestinationEnabled(
                true,
                "IMMICH",
                "cloud/IMMICH/Album",
                sources,
                isCloudAlbum = true,
            )
        )
        assertTrue(
            isAlbumMoveDestinationEnabled(
                true,
                "external_primary",
                "cloud/LocalAlbum/",
                sources,
                isCloudAlbum = false,
            )
        )
    }

    @Test
    fun restrictedSourceStaysEnabledForWritableAlbum() {
        // A WhatsApp image (Android/media/com.whatsapp/...) can be copied into an existing album,
        // and moved there through the copy + delete-request path.
        val restrictedSource = listOf(
            AlbumDestinationSource(
                "external_primary",
                "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/",
            ),
        )

        assertTrue(
            isAlbumCopyDestinationEnabled(
                hasFullMediaAccess = false,
                albumRelativePath = "Pictures/Target/",
            )
        )
        assertTrue(
            isAlbumMoveDestinationEnabled(
                false,
                "external_primary",
                "Pictures/Target/",
                restrictedSource,
            )
        )
    }

    @Test
    fun copyToRestrictedOrCloudAlbumStaysDisabled() {
        assertFalse(
            isAlbumCopyDestinationEnabled(
                hasFullMediaAccess = false,
                albumRelativePath = "Android/media/example.app/Pictures/",
            )
        )
        assertFalse(
            isAlbumCopyDestinationEnabled(
                hasFullMediaAccess = true,
                albumRelativePath = "cloud/IMMICH/Album",
                isCloudAlbum = true,
            )
        )
        assertTrue(
            isAlbumCopyDestinationEnabled(
                hasFullMediaAccess = true,
                albumRelativePath = "Android/media/example.app/Pictures/",
            )
        )
    }

    @Test
    fun moveDisablesExactSourceFolderButAllowsDifferentSameNamedFolder() {
        val sources = listOf(AlbumDestinationSource("external_primary", "DCIM/Camera/"))

        assertFalse(
            isAlbumMoveDestinationEnabled(
                false,
                "external_primary",
                "DCIM/Camera/",
                sources,
            )
        )
        assertTrue(
            isAlbumMoveDestinationEnabled(
                false,
                "external_primary",
                "Pictures/Camera/",
                sources,
            )
        )
        assertTrue(
            isAlbumMoveDestinationEnabled(
                false,
                "external_primary",
                "DCIM/Camera/",
                sources + AlbumDestinationSource("external_primary", "Pictures/Camera/"),
            )
        )
    }

    @Test
    fun cloudOnlySelectionCanTargetWritableLocalAlbum() {
        assertTrue(
            isAlbumMoveDestinationEnabled(
                false,
                "external_primary",
                "Pictures/Target/",
                emptyList(),
            )
        )
    }

    @Test
    fun albumAbsolutePathUsesFilesystemRootInsteadOfMediaStoreVolumeName() {
        assertEquals(
            "/storage/emulated/0/Pictures/Target/",
            resolveAlbumAbsolutePath(
                pathToThumbnail = "/storage/emulated/0/Pictures/Target/photo.jpg",
                relativePath = "Pictures/Target/",
                storageVolume = "external_primary",
            )
        )
    }

    @Test
    fun virtualParentAbsolutePathUsesItsRelativePath() {
        assertEquals(
            "/storage/emulated/0/Pictures/Parent/",
            resolveAlbumAbsolutePath(
                pathToThumbnail = "/storage/emulated/0/Pictures/Parent/Child/photo.jpg",
                relativePath = "Pictures/Parent/",
                storageVolume = "external_primary",
            )
        )
    }

    @Test
    fun sdAlbumAbsolutePathRetainsMountedFilesystemRoot() {
        assertEquals(
            "/storage/71F8-2C0A/Pictures/Target/",
            resolveAlbumAbsolutePath(
                pathToThumbnail = "/storage/71F8-2C0A/Pictures/Target/photo.jpg",
                relativePath = "Pictures/Target/",
                storageVolume = "71f8-2c0a",
            )
        )
    }

    @Test
    fun unresolvedSdAlbumDoesNotFallBackToPrimaryStorage() {
        assertEquals(
            "",
            resolveAlbumAbsolutePath(
                pathToThumbnail = "",
                relativePath = "Pictures/Target/",
                storageVolume = "71f8-2c0a",
            )
        )
    }
}
