/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components.util

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StickyHeaderGrid(
    modifier: Modifier = Modifier,
    showSearchBar: Boolean,
    searchBarPadding: Dp,
    lazyState: LazyGridState,
    headerMatcher: (LazyGridItemInfo) -> Boolean,
    stickyHeader: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val statusBarHeightPx = WindowInsets.statusBars.getTop(LocalDensity.current)
    val searchBarOffset = with(LocalDensity.current) {
        return@with if (showSearchBar)
            28.sp.roundToPx() + searchBarPadding.roundToPx() else 0
    }
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
                    .let { difference -> if (difference < 0) 0 else -difference - searchBarOffset }
            }
        }
    }
    val toolbarOffset = with(LocalDensity.current) {
        return@with remember(LocalDensity.current) {
            if (showSearchBar) 0 else 64.dp.roundToPx() + statusBarHeightPx
        }
    }
    val offsetAnimation by animateIntOffsetAsState(
        IntOffset(x = 0, y = headerOffset + toolbarOffset), label = "offsetAnimation"
    )

    Box(modifier = modifier) {
        content()
        Box(
            modifier = Modifier.offset { offsetAnimation }
        ) {
            stickyHeader()
        }
    }
}