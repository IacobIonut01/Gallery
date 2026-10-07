/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import kotlinx.coroutines.flow.MutableStateFlow

interface MediaSelector {
    val selectedMedia: MutableStateFlow<Set<Long>>
    val isSelectionActive: MutableStateFlow<Boolean>

    fun clearSelection()

    fun <T: Media> toggleSelection(
        mediaState: MediaState<T>,
        index: Int
    )

    fun <T: Media> toggleSelectionById(
        mediaState: MediaState<T>,
        mediaId: Long
    )

    fun addToSelection(
        list: List<Long>
    )

    fun removeFromSelection(
        list: List<Long>
    )

    fun rawUpdateSelection(
        list: Set<Long>
    )
}