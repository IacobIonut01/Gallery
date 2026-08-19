/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.collection

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.cloudAlbumId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionAlbumSelectorTest {

    @Test
    fun cloudAlbums_areExcludedFromCollectionSelection() {
        val computedCloudAlbumId = cloudAlbumId(ProviderType.IMMICH, 42L, "remote-album")

        assertFalse(isSelectableCollectionAlbumId(computedCloudAlbumId))
        assertTrue(isSelectableCollectionAlbumId(0L))
        assertTrue(isSelectableCollectionAlbumId(42L))
    }
}
