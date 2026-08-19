/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.common.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaContentStateTest {
    @Test
    fun errorHasPrecedenceOverLoadingAndContent() {
        assertEquals(
            MediaContentState.ERROR,
            mediaContentState(isLoading = true, error = "failed", isEmpty = false),
        )
    }

    @Test
    fun statesAreResolvedExclusively() {
        assertEquals(
            MediaContentState.LOADING,
            mediaContentState(isLoading = true, error = "", isEmpty = true),
        )
        assertEquals(
            MediaContentState.EMPTY,
            mediaContentState(isLoading = false, error = "", isEmpty = true),
        )
        assertEquals(
            MediaContentState.CONTENT,
            mediaContentState(isLoading = false, error = "", isEmpty = false),
        )
    }
}
