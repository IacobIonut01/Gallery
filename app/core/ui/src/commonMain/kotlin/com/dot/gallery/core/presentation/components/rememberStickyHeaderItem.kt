/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dot.gallery.core.ui.generated.resources.Res
import com.dot.gallery.core.ui.generated.resources.header_today
import com.dot.gallery.core.ui.generated.resources.header_yesterday
import com.dot.gallery.core.ui.util.rememberedDerivedState
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.isHeaderKey
import com.dot.gallery.domain.model.isSmallHeaderKey
import org.jetbrains.compose.resources.stringResource

data class StickyHeaderInfo(
    val text: String,
    val mediaIds: Set<Long>,
)

private val StickyHeaderInfoSaver = listSaver<StickyHeaderInfo?, Any>(
    save = { info ->
        if (info == null) emptyList()
        else listOf(info.text) + info.mediaIds.toList()
    },
    restore = { list ->
        if (list.isEmpty()) null
        else StickyHeaderInfo(
            text = list[0] as String,
            mediaIds = list.drop(1).map { it as Long }.toSet(),
        )
    }
)

@Composable
fun <T : Media> rememberStickyHeaderItem(
    gridState: LazyGridState,
    mediaState: State<MediaState<T>>
): State<StickyHeaderInfo?> {
    val stringToday = stringResource(Res.string.header_today)
    val stringYesterday = stringResource(Res.string.header_yesterday)

    var lastStickyHeaderItem by rememberSaveable(stateSaver = StickyHeaderInfoSaver) {
        mutableStateOf(null)
    }

    /**
     * Remember last known header item
     */
    val stickyHeaderLastItem = rememberedDerivedState(gridState) {
        val visibleItems = gridState.layoutInfo.visibleItemsInfo
        val headers = mediaState.value.headers
        val firstItem = visibleItems.firstOrNull()
        val firstVisibleSmallHeaderKey = visibleItems.firstOrNull { it.key.isSmallHeaderKey }?.key?.toString()

        val item = firstVisibleSmallHeaderKey?.let { key -> headers.firstOrNull { it.key == key } }
        return@rememberedDerivedState if (item != null) {
            val localize: (String) -> String = {
                it.replace("Today", stringToday).replace("Yesterday", stringYesterday)
            }
            val newIndex = (headers.indexOf(item) - 1).coerceAtLeast(0)
            val previousHeader = headers[newIndex]
            val stickyHeader = if (firstItem != null && !firstItem.key.isHeaderKey) {
                previousHeader
            } else {
                item
            }
            val info = StickyHeaderInfo(
                text = localize(stickyHeader.text),
                mediaIds = stickyHeader.data,
            )
            lastStickyHeaderItem = info
            info
        } else lastStickyHeaderItem
    }
    return stickyHeaderLastItem
}
