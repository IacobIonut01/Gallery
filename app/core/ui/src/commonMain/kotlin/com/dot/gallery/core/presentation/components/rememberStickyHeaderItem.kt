package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.isSmallHeaderKey
import com.dot.gallery.domain.model.isHeaderKey
import com.dot.gallery.core.ui.util.rememberedDerivedState
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.core.ui.generated.resources.Res
import com.dot.gallery.core.ui.generated.resources.header_today
import com.dot.gallery.core.ui.generated.resources.header_yesterday



@Composable
fun <T : Media> rememberStickyHeaderItem(
    gridState: LazyGridState,
    mediaState: State<MediaState<T>>
): State<String?> {
    val stringToday = stringResource(Res.string.header_today)
    val stringYesterday = stringResource(Res.string.header_yesterday)

    var lastStickyHeaderItem by rememberSaveable { mutableStateOf<String?>(null) }

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
            val newItem = item.text
                .replace("Today", stringToday)
                .replace("Yesterday", stringYesterday)
            val newIndex = (headers.indexOf(item) - 1).coerceAtLeast(0)
            val previousHeader = headers[newIndex].text
                .replace("Today", stringToday)
                .replace("Yesterday", stringYesterday)
            val newHeader = if (firstItem != null && !firstItem.key.isHeaderKey) {
                previousHeader
            } else {
                newItem
            }
            lastStickyHeaderItem = newHeader
            newHeader
        } else lastStickyHeaderItem
    }
    return stickyHeaderLastItem
}