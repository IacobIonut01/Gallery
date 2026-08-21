/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.netfs

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.netfs.bridge.NetFsByteRange
import com.dot.gallery.cloud.netfs.bridge.NetFsLoopbackRoute
import com.dot.gallery.cloud.netfs.bridge.buildNetFsLoopbackPath
import com.dot.gallery.cloud.netfs.bridge.parseNetFsByteRange
import com.dot.gallery.cloud.netfs.bridge.parseNetFsLoopbackRoute
import com.dot.gallery.core.usesLiveCloudAlbumMembership
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkFileSystemSupportTest {

    private val entries = listOf(
        entry("Trips/one.jpg", 100L),
        entry("Trips/2025/two.jpg", 300L),
        entry("Trips-old/other.jpg", 400L),
        entry("Family/photo.jpg", 200L)
    )

    @Test
    fun mediaIndexReusesStablePagesAndScopesAlbumPaths() {
        val index = NetFsMediaIndex(entries)

        assertEquals(entries.take(2), index.page(0, 2))
        assertEquals(entries.drop(2).take(2), index.page(1, 2))
        assertEquals(listOf("Family", "Trips", "Trips-old"), index.rootAlbumPaths())
        assertEquals(
            listOf("Trips/one.jpg", "Trips/2025/two.jpg"),
            index.inAlbum("Trips").map { it.relativePath }
        )
    }

    @Test
    fun mediaIndexBuildsAlbumCountAndNewestCover() {
        val stats = NetFsMediaIndex(entries).albumStats("Trips")

        assertEquals(2, stats.assetCount)
        assertEquals("Trips/2025/two.jpg", stats.thumbnailAssetId)
    }

    @Test
    fun nasAlbumMembershipDoesNotWaitForPartialRoomIndex() {
        assertTrue(usesLiveCloudAlbumMembership(ProviderType.SMB))
        assertTrue(usesLiveCloudAlbumMembership(ProviderType.NFS))
        assertFalse(usesLiveCloudAlbumMembership(ProviderType.IMMICH))
    }

    @Test
    fun byteRangesSupportOpenEndedSuffixAndClampedRequests() {
        assertEquals(NetFsByteRange(100L, 999L), parseNetFsByteRange("bytes=100-", 1_000L))
        assertEquals(NetFsByteRange(900L, 999L), parseNetFsByteRange("bytes=-100", 1_000L))
        assertEquals(NetFsByteRange(100L, 999L), parseNetFsByteRange("bytes=100-2000", 1_000L))
        assertNull(parseNetFsByteRange("bytes=1000-", 1_000L))
        assertNull(parseNetFsByteRange("bytes=200-100", 1_000L))
        assertNull(parseNetFsByteRange("bytes=0-1,4-5", 1_000L))
    }

    @Test
    fun loopbackRouteRetainsAccountAndFullPath() {
        val path = "Photos/2026/Summer & winter/ä.jpg"
        val routePath = buildNetFsLoopbackPath(
            token = "token",
            providerType = ProviderType.SMB,
            configId = 42L,
            kind = "original",
            sizeName = "orig",
            path = path
        )

        assertEquals(
            NetFsLoopbackRoute(ProviderType.SMB, 42L, "original", "orig", path),
            parseNetFsLoopbackRoute(routePath, "token")
        )
        assertNull(parseNetFsLoopbackRoute(routePath, "different-token"))
    }

    private fun entry(path: String, modified: Long) = NetFsEntry(
        name = path.substringAfterLast('/'),
        relativePath = path,
        isDirectory = false,
        size = 1L,
        lastModified = modified
    )
}
