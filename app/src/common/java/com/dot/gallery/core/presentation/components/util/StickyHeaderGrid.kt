package com.dot.gallery.core.presentation.components.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun StickyHeaderGrid(
    modifier: Modifier = Modifier,
    lazyState: LazyGridState,
    headerMatcher: (LazyGridItemInfo) -> Boolean,
    stickyHeader: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val headerOffset by remember(remember { derivedStateOf { lazyState.layoutInfo } }) {
        derivedStateOf {
            val layoutInfo = lazyState.layoutInfo
            val startOffset = layoutInfo.viewportStartOffset
            val firstCompletelyVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull {
                it.offset.y >= startOffset
            } ?: return@derivedStateOf 0

            when (headerMatcher(firstCompletelyVisibleItem)) {
                false -> 0
                true -> firstCompletelyVisibleItem.size
                    .height
                    .minus(firstCompletelyVisibleItem.offset.y)
                    .let { difference -> if (difference < 0) 0 else -difference }
            }
        }
    }

    Box(modifier = modifier) {
        content()
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .offset { IntOffset(x = 0, y = headerOffset + 64.dp.roundToPx()) }
        ) {
            stickyHeader()
        }
    }
}