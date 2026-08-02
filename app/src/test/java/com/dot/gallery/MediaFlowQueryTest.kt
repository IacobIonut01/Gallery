/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery

import com.dot.gallery.feature_node.data.data_source.mediastore.queries.mediaBucketSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFlowQueryTest {

    @Test
    fun bucketSelectionIsStableAndDeduplicated() {
        assertEquals("((bucket_id = ?) OR (bucket_id = ?)) OR (bucket_id = ?)", mediaBucketSelection(3))
    }

    @Test
    fun largeBucketSelectionUsesBoundArguments() {
        val selection = mediaBucketSelection(1_500)

        assertEquals(1_500, selection.count { it == '?' })
        assertTrue(selection.contains("bucket_id = ?"))
    }
}
