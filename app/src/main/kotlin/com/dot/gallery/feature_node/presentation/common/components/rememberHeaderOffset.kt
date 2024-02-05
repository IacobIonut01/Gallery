package com.dot.gallery.feature_node.presentation.common.components

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
fun rememberHeaderOffset(
    lazyGridState: LazyGridState,
    headerMatcher: (LazyGridItemInfo) -> Boolean,
    searchBarOffset: Int
): State<Int> {
    return remember {
        derivedStateOf {
            val layoutInfo = lazyGridState.layoutInfo
            val startOffset = layoutInfo.viewportStartOffset
            val firstCompletelyVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull {
                it.offset.y >= startOffset
            }
            when (firstCompletelyVisibleItem?.let { headerMatcher(it) }) {
                true -> firstCompletelyVisibleItem.size
                    .height
                    .minus(firstCompletelyVisibleItem.offset.y)
                    .let { difference -> if (difference < 0) 0 else -difference - searchBarOffset }
                else -> 0
            }
        }
    }
}