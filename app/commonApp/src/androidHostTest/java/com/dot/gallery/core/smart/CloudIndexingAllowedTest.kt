/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class CloudIndexingAllowedTest {

    @Test
    fun localContentUrisAreAlwaysAllowed() {
        assertTrue(cloudIndexingAllowed("content://media/external/images/media/42", emptySet()))
        assertTrue(cloudIndexingAllowed("file:///storage/emulated/0/DCIM/a.jpg", emptySet()))
    }

    @Test
    fun cloudUrisAreSkippedByDefault() {
        assertFalse(
            cloudIndexingAllowed(
                "cloud://IMMICH/abc-123?size=preview&cfg=7",
                emptySet()
            )
        )
    }

    @Test
    fun optedInProviderTypeIsAllowed() {
        assertTrue(
            cloudIndexingAllowed(
                "cloud://IMMICH/abc-123?size=preview&cfg=7",
                setOf("IMMICH")
            )
        )
        assertFalse(
            cloudIndexingAllowed(
                "cloud://WEBDAV/photos/a.jpg?size=preview&cfg=3",
                setOf("IMMICH")
            )
        )
        assertTrue(
            cloudIndexingAllowed(
                "cloud://WEBDAV/photos/a.jpg?size=preview&cfg=3",
                setOf("IMMICH", "WEBDAV")
            )
        )
    }

    @Test
    fun unknownOrMalformedUrisAreAllowed() {
        // A URI that doesn't parse as cloud belongs to the local pool; never silently
        // drop it from indexing.
        assertTrue(cloudIndexingAllowed("notauri", setOf("IMMICH")))
        assertTrue(cloudIndexingAllowed("cloud://", emptySet()))
    }
}
