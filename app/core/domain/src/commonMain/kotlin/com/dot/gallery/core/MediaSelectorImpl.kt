/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import kotlinx.coroutines.flow.MutableStateFlow

class MediaSelectorImpl : MediaSelector {

    override val selectedMedia = MutableStateFlow<Set<Long>>(emptySet())
    override val isSelectionActive = MutableStateFlow(false)

    override fun <T: Media> toggleSelection(
        mediaState: MediaState<T>,
        index: Int
    ) {
        val item = mediaState.media[index]
        toggleSelectionById(mediaState, item.id)
    }

    override fun <T: Media> toggleSelectionById(
        mediaState: MediaState<T>,
        mediaId: Long
    ) {
        val groupIds = mediaState.mediaGroups[mediaId]?.map { it.id }
        val idsToToggle = groupIds ?: listOf(mediaId)
        val isCurrentlySelected = selectedMedia.value.contains(mediaId)
        val newSelection = if (isCurrentlySelected) {
            selectedMedia.value.toMutableSet().apply { removeAll(idsToToggle.toSet()) }
        } else {
            selectedMedia.value.toMutableSet().apply { addAll(idsToToggle) }
        }
        selectedMedia.tryEmit(newSelection)
        isSelectionActive.value = newSelection.isNotEmpty()
    }

    override fun addToSelection(list: List<Long>) {
        val newSelection = selectedMedia.value.toMutableSet().apply { addAll(list) }
        selectedMedia.tryEmit(newSelection)
        isSelectionActive.value = newSelection.isNotEmpty()
    }

    override fun removeFromSelection(list: List<Long>) {
        val newSelection = selectedMedia.value.toMutableSet().apply { removeAll(list) }
        selectedMedia.tryEmit(newSelection)
        isSelectionActive.value = newSelection.isNotEmpty()
    }

    override fun rawUpdateSelection(list: Set<Long>) {
        selectedMedia.tryEmit(list)
        isSelectionActive.tryEmit(list.isNotEmpty())
    }

    override fun clearSelection() {
        selectedMedia.tryEmit(emptySet())
        isSelectionActive.value = false
    }
}