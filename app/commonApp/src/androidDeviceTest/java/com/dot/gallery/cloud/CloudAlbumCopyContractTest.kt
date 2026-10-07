/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudAlbumIdentity
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteNameConflictPolicy
import com.dot.gallery.cloud.sync.CloudAlbumTransferMode
import com.dot.gallery.domain.model.Album
import com.dot.gallery.feature.exif.ui.CloudDestinationAccount
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.cloud.core.matches
import com.dot.gallery.core.startup.toAlbum
import com.dot.gallery.cloud.sync.CloudAlbumCopyRequestStore
import com.dot.gallery.domain.uri.path
import com.dot.gallery.feature.exif.ui.buildTransferDestinationSections


@RunWith(AndroidJUnit4::class)
class CloudAlbumCopyContractTest {

    @Test
    fun cloudAlbumMappingPreservesWritableIdentityWithoutThumbnail() {
        val source = CloudAlbum(
            remoteId = "Photos/Family",
            providerType = ProviderType.SMB,
            serverConfigId = 42L,
            name = "Family",
            assetCount = 0
        )

        val album = source.toAlbum()

        assertEquals(
            CloudAlbumIdentity(ProviderType.SMB, 42L, "Photos/Family"),
            album.cloudIdentity
        )
        assertTrue(requireNotNull(album.cloudIdentity).matches(album.id))
    }

    @Test
    fun destinationSectionsSeparateDeviceAndCloudAccounts() {
        val local = Album(
            id = 1L,
            label = "Camera",
            uri = Uri.EMPTY,
            pathToThumbnail = "/storage/emulated/0/DCIM/Camera/photo.jpg",
            relativePath = "DCIM/Camera",
            timestamp = 0L
        )
        val firstCloud = CloudAlbum(
            remoteId = "Photos",
            providerType = ProviderType.SMB,
            serverConfigId = 42L,
            name = "Photos",
            assetCount = 1
        ).toAlbum()
        val secondCloud = CloudAlbum(
            remoteId = "Archive",
            providerType = ProviderType.SMB,
            serverConfigId = 84L,
            name = "Archive",
            assetCount = 1
        ).toAlbum()

        val sections = buildTransferDestinationSections(
            albums = listOf(secondCloud, local, firstCloud),
            accounts = mapOf(
                42L to CloudDestinationAccount(ProviderType.SMB, "Home NAS", "gallery · nas.local"),
                84L to CloudDestinationAccount(ProviderType.SMB, "Archive NAS", "archive.local")
            ),
            localTitle = "On this device",
            unavailableTitle = "Unavailable"
        )

        assertEquals(listOf("local", "cloud-84", "cloud-42"), sections.map { it.key })
        assertEquals(listOf("Camera"), sections[0].albums.map { it.label })
        assertEquals("Archive NAS", sections[1].title)
        assertEquals("Home NAS", sections[2].title)
    }

    @Test
    fun requestStoreRoundTripsMediaAndDestination() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = CloudAlbumCopyRequestStore(context)
        val identity = CloudAlbumIdentity(ProviderType.SMB, 42L, "Photos/Family")
        val media = UriMedia(
            id = 7L,
            label = "photo.jpg",
            uri = Uri.parse("content://media/external/images/media/7"),
            path = "",
            relativePath = "Pictures",
            albumID = 3L,
            albumLabel = "Pictures",
            timestamp = 1L,
            fullDate = "",
            mimeType = "image/jpeg",
            favorite = 0,
            trashed = 0,
            size = 10L
        )
        val request = store.create(
            albumId = com.dot.gallery.cloud.core.cloudAlbumId(
                identity.providerType,
                identity.serverConfigId,
                identity.remoteId
            ),
            destination = identity,
            destinationLabel = "Family (SMB)",
            media = listOf(media),
            mode = CloudAlbumTransferMode.MOVE,
            conflictPolicy = RemoteNameConflictPolicy.KEEP_BOTH
        )

        try {
            val restored = requireNotNull(store.read(request.id))
            assertEquals(identity, restored.destination)
            assertEquals(CloudAlbumTransferMode.MOVE, restored.mode)
            assertEquals("photo.jpg", restored.items.single().media.label)
            assertEquals(media.uri, (restored.items.single().media as UriMedia).uri)
        } finally {
            store.delete(request.id)
        }
    }
}
