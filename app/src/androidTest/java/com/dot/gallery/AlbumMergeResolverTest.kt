/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.AlbumMergeReason
import com.dot.gallery.feature_node.domain.model.AlbumMergeResolver
import com.dot.gallery.feature_node.domain.model.MergedSubfolderAlbum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumMergeResolverTest {

    @Test
    fun mergeSubfoldersIncludesAllDescendantsOnSameVolume() {
        val root = album(1, "/storage/emulated/0", "Pictures/Trip/", 1)
        val child = album(2, "/storage/emulated/0", "Pictures/Trip/Day1/", 2)
        val grandchild = album(3, "/storage/emulated/0", "Pictures/Trip/Day1/Edited/", 3)
        val otherVolume = album(4, "/storage/1234-5678", "Pictures/Trip/Day2/", 4)

        val result = AlbumMergeResolver.mergeSubfolders(
            listOf(root, child, grandchild, otherVolume),
            listOf(config(root))
        )

        assertEquals(2, result.size)
        val merged = result.first { it.id == root.id }
        assertEquals(listOf(1L, 2L, 3L), merged.sourceAlbumIds)
        assertEquals(6L, merged.count)
        assertTrue(merged.mergesSubfolders)
        assertTrue(result.any { it.id == otherVolume.id })
    }

    @Test
    fun outerConfiguredRootOwnsNestedConfiguredRootDeterministically() {
        val root = album(1, "/storage/emulated/0", "Pictures/", 1)
        val child = album(2, "/storage/emulated/0", "Pictures/Trip/", 2)
        val grandchild = album(3, "/storage/emulated/0", "Pictures/Trip/Day1/", 3)
        val configs = listOf(config(child), config(root))

        val result = AlbumMergeResolver.mergeSubfolders(
            listOf(grandchild, child, root),
            configs
        )

        assertEquals(1, result.size)
        assertEquals(root.id, result.single().id)
        assertEquals(setOf(1L, 2L, 3L), result.single().sourceAlbumIds.toSet())
    }

    @Test
    fun mergeByNameLeavesManualFolderMergeIndependent() {
        val root = album(1, "/storage/emulated/0", "Pictures/Trip/", 1)
        val child = album(2, "/storage/emulated/0", "Pictures/Trip/Day1/", 2)
        val sameName = album(3, "/storage/1234-5678", "Photos/Trip/", 3)
        val manuallyMerged = AlbumMergeResolver.mergeSubfolders(
            listOf(root, child),
            listOf(config(root))
        ).single()

        val result = AlbumMergeResolver.mergeByName(listOf(manuallyMerged, sameName))

        assertEquals(2, result.size)
        val mergedFolder = result.first { it.id == root.id }
        assertEquals(setOf(1L, 2L), mergedFolder.sourceAlbumIds.toSet())
        assertTrue(AlbumMergeReason.SUBFOLDERS in mergedFolder.mergeReasons)
        assertFalse(AlbumMergeReason.SAME_NAME in mergedFolder.mergeReasons)
    }

    @Test
    fun virtualParentRepresentsFolderWithoutDirectMedia() {
        val child = album(2, "/storage/emulated/0", "Pictures/Trip/Day1/", 2)
        val sibling = album(3, "/storage/emulated/0", "Pictures/Trip/Day2/", 3)
        val parentPath = "Pictures/Trip/"
        val config = MergedSubfolderAlbum(
            id = AlbumMergeResolver.virtualAlbumId(child.volume, parentPath),
            folderKey = MergedSubfolderAlbum.folderKey(child.volume, parentPath),
            volume = child.volume,
            relativePath = parentPath
        )

        val merged = AlbumMergeResolver.mergeSubfolders(listOf(child, sibling), listOf(config)).single()

        assertEquals("Trip", merged.label)
        assertEquals(setOf(2L, 3L), merged.sourceAlbumIds.toSet())
        assertTrue(AlbumMergeResolver.isVirtualAlbumId(merged.id))
        assertFalse(merged.sourceAlbumIds.contains(merged.id))
    }

    private fun config(album: Album) = MergedSubfolderAlbum(
        id = album.id,
        folderKey = MergedSubfolderAlbum.folderKey(album.volume, album.relativePath),
        volume = album.volume,
        relativePath = album.relativePath
    )

    private fun album(
        id: Long,
        volume: String,
        relativePath: String,
        count: Long
    ) = Album(
        id = id,
        label = relativePath.trim('/').substringAfterLast('/'),
        uri = Uri.parse("content://media/$id"),
        pathToThumbnail = "$volume/${relativePath}image$id.jpg",
        relativePath = relativePath,
        timestamp = id,
        count = count,
        size = count * 10
    )
}
