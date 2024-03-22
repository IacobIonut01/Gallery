/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.common.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dokar.pinchzoomgrid.PinchZoomGridScope
import com.dot.gallery.R
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.presentation.components.StickyHeader
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isBigHeaderKey
import com.dot.gallery.feature_node.presentation.util.FeedbackManager
import com.dot.gallery.feature_node.presentation.util.update
import com.example.compose_recyclerview.ComposeRecyclerView
import com.example.compose_recyclerview.adapter.ComposeRecyclerViewAdapter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PinchZoomGridScope.MediaGridView(
    mediaState: MediaState,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    searchBarPaddingTop: Dp = 0.dp,
    showSearchBar: Boolean = false,
    allowSelection: Boolean = false,
    selectionState: MutableState<Boolean> = remember { mutableStateOf(false) },
    selectedMedia: SnapshotStateList<Media> = remember { mutableStateListOf() },
    toggleSelection: (Int) -> Unit = {},
    canScroll: Boolean = true,
    allowHeaders: Boolean = remember { true },
    enableStickyHeaders: Boolean = false,
    showMonthlyHeader: Boolean = false,
    aboveGridContent: @Composable (() -> Unit)? = null,
    isScrolling: MutableState<Boolean>,
    onMediaClick: (media: Media) -> Unit = {}
) {
    val stringToday = stringResource(id = R.string.header_today)
    val stringYesterday = stringResource(id = R.string.header_yesterday)

    val scope = rememberCoroutineScope()
    val mappedData = remember(showMonthlyHeader, mediaState) {
        if (showMonthlyHeader) mediaState.mappedMediaWithMonthly
        else mediaState.mappedMedia
    }

    /** Selection state handling **/
    BackHandler(
        enabled = selectionState.value && allowSelection,
        onBack = {
            selectionState.value = false
            selectedMedia.clear()
        }
    )
    /** ************ **/

    val feedbackManager = FeedbackManager.rememberFeedbackManager()

    @Composable
    fun mediaGrid() {
        /*LaunchedEffect(gridState.isScrollInProgress) {
            isScrolling.value = gridState.isScrollInProgress
        }*/
        Box {


            /*LazyVerticalGrid(
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                columns = gridCells,
                contentPadding = paddingValues,
                userScrollEnabled = canScroll,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                if (aboveGridContent != null) {
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "aboveGrid"
                    ) {
                        aboveGridContent.invoke()
                    }
                }

                if (allowHeaders) {
                    items(
                        items = mappedData,
                        key = { item -> item.key },
                        contentType = { item -> item.key.startsWith("media_") },
                        span = { item ->
                            GridItemSpan(if (item.key.isHeaderKey) maxLineSpan else 1)
                        }
                    ) { it ->
                        val item = remember {
                            if (it is MediaItem.Header) it
                            else it as MediaItem.MediaViewItem
                        }
                        if (item is MediaItem.Header) {
                            val isChecked = rememberSaveable { mutableStateOf(false) }
                            if (allowSelection) {
                                LaunchedEffect(selectionState.value) {
                                    // Uncheck if selectionState is set to false
                                    isChecked.value = isChecked.value && selectionState.value
                                }
                                LaunchedEffect(selectedMedia.size) {
                                    // Partial check of media items should not check the header
                                    isChecked.value = selectedMedia.containsAll(item.data)
                                }
                            }
                            StickyHeader(
                                modifier = Modifier.pinchItem(key = it.key),
                                date = remember {
                                    item.text
                                        .replace("Today", stringToday)
                                        .replace("Yesterday", stringYesterday)
                                },
                                showAsBig = remember { item.key.isBigHeaderKey },
                                isCheckVisible = selectionState,
                                isChecked = isChecked
                            ) {
                                if (allowSelection) {
                                    feedbackManager.vibrate()
                                    scope.launch {
                                        isChecked.value = !isChecked.value
                                        if (isChecked.value) {
                                            val toAdd = item.data.toMutableList().apply {
                                                // Avoid media from being added twice to selection
                                                removeIf { selectedMedia.contains(it) }
                                            }
                                            selectedMedia.addAll(toAdd)
                                        } else selectedMedia.removeAll(item.data)
                                        selectionState.update(selectedMedia.isNotEmpty())
                                    }
                                }
                            }
                        } else {
                            MediaImage(
                                modifier = Modifier
                                    .animateItemPlacement()
                                    .pinchItem(key = it.key),
                                media = (item as MediaItem.MediaViewItem).media,
                                selectionState = selectionState,
                                selectedMedia = selectedMedia,
                                canClick = canScroll,
                                onItemClick = {
                                    if (selectionState.value && allowSelection) {
                                        feedbackManager.vibrate()
                                        toggleSelection(mediaState.media.indexOf(it))
                                    } else onMediaClick(it)
                                }
                            ) {
                                if (allowSelection) {
                                    feedbackManager.vibrate()
                                    toggleSelection(mediaState.media.indexOf(it))
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = mediaState.media,
                        key = { _, item -> item.toString() },
                        contentType = { _, item -> item.isImage }
                    ) { index, media ->
                        MediaImage(
                            modifier = Modifier
                                .animateItemPlacement()
                                .pinchItem(key = media.toString()),
                            media = media,
                            selectionState = selectionState,
                            selectedMedia = selectedMedia,
                            canClick = canScroll,
                            onItemClick = {
                                if (selectionState.value && allowSelection) {
                                    feedbackManager.vibrate()
                                    toggleSelection(index)
                                } else onMediaClick(it)
                            },
                            onItemLongClick = {
                                if (allowSelection) {
                                    feedbackManager.vibrate()
                                    toggleSelection(index)
                                }
                            }
                        )
                    }
                }
            }

            if (allowHeaders) {
                TimelineScroller(
                    gridState = gridState,
                    mappedData = mappedData,
                    paddingValues = paddingValues
                )
            }*/
        }
    }

    val items = remember(allowHeaders, mappedData, mediaState) {
        if (allowHeaders) mappedData else mediaState.media
    }
    val context = LocalContext.current
    val layoutManager = remember {
        GridLayoutManager(context, 4).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return items.getOrNull(position)?.let {
                        if (it is MediaItem.Header) 4 else 1
                    } ?: 1
                }
            }
        }
    }

    val density = LocalDensity.current
    val topPadding = remember(paddingValues, density) {
        with(density) {
            paddingValues.calculateTopPadding().roundToPx()
        }
    }
    val bottomPadding = remember(paddingValues, density) {
        with(density) {
            paddingValues.calculateBottomPadding().roundToPx()
        }
    }

    ComposeRecyclerView(
        modifier = Modifier.fillMaxSize(),
        items = items,
        itemBuilder = { item, index ->
            when (item) {
                is MediaItem -> {
                    when (item) {
                        is MediaItem.Header -> {
                            val isChecked = rememberSaveable { mutableStateOf(false) }
                            if (allowSelection) {
                                LaunchedEffect(selectionState.value) {
                                    // Uncheck if selectionState is set to false
                                    isChecked.value = isChecked.value && selectionState.value
                                }
                                LaunchedEffect(selectedMedia.size) {
                                    // Partial check of media items should not check the header
                                    isChecked.value = selectedMedia.containsAll(item.data)
                                }
                            }
                            StickyHeader(
                                modifier = Modifier,
                                date = remember {
                                    item.text
                                        .replace("Today", stringToday)
                                        .replace("Yesterday", stringYesterday)
                                },
                                showAsBig = remember { item.key.isBigHeaderKey },
                                isCheckVisible = selectionState,
                                isChecked = isChecked
                            ) {
                                if (allowSelection) {
                                    feedbackManager.vibrate()
                                    scope.launch {
                                        isChecked.value = !isChecked.value
                                        if (isChecked.value) {
                                            val toAdd = item.data.toMutableList().apply {
                                                // Avoid media from being added twice to selection
                                                removeIf { selectedMedia.contains(it) }
                                            }
                                            selectedMedia.addAll(toAdd)
                                        } else selectedMedia.removeAll(item.data)
                                        selectionState.update(selectedMedia.isNotEmpty())
                                    }
                                }
                            }
                            return@ComposeRecyclerView
                        }

                        is MediaItem.MediaViewItem -> {
                            MediaImage(
                                modifier = Modifier,
                                media = item.media,
                                selectionState = selectionState,
                                selectedMedia = selectedMedia,
                                canClick = canScroll,
                                onItemClick = {
                                    if (selectionState.value && allowSelection) {
                                        feedbackManager.vibrate()
                                        toggleSelection(mediaState.media.indexOf(it))
                                    } else onMediaClick(it)
                                }
                            ) {
                                if (allowSelection) {
                                    feedbackManager.vibrate()
                                    toggleSelection(mediaState.media.indexOf(it))
                                }
                            }

                            return@ComposeRecyclerView
                        }
                    }
                }

                is Media -> {
                    MediaImage(
                        modifier = Modifier,
                        media = item,
                        selectionState = selectionState,
                        selectedMedia = selectedMedia,
                        canClick = canScroll,
                        onItemClick = {
                            if (selectionState.value && allowSelection) {
                                feedbackManager.vibrate()
                                toggleSelection(index)
                            } else onMediaClick(it)
                        },
                        onItemLongClick = {
                            if (allowSelection) {
                                feedbackManager.vibrate()
                                toggleSelection(index)
                            }
                        }
                    )
                    return@ComposeRecyclerView
                }
            }
        },
        itemTypeBuilder = object : ComposeRecyclerViewAdapter.ItemTypeBuilder {
            override fun getItemType(position: Int): Int {
                return when (items.getOrNull(position)) {
                    is MediaItem.Header -> 4
                    else -> 1
                }
            }
        },
        onCreate = {
            it.clipToPadding = false
            it.setPadding(
                it.paddingLeft,
                topPadding,
                it.paddingRight,
                bottomPadding
            )
            it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    isScrolling.value = newState != RecyclerView.SCROLL_STATE_IDLE
                }
            })
        }
    )

    /*    if (enableStickyHeaders) {
            */
    /**
     * Remember last known header item
     *//*
        val stickyHeaderLastItem = remember { mutableStateOf<String?>(null) }

        val headers = remember(mappedData) {
            mappedData.filterIsInstance<MediaItem.Header>().filter { !it.key.isBigHeaderKey }
        }

        val stickyHeaderItem by remember(mappedData) {
            derivedStateOf {
                val firstItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull()
                val firstHeaderIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull {
                    it.key.isHeaderKey && !it.key.toString().contains("big")
                }?.index

                val item = firstHeaderIndex?.let(mappedData::getOrNull)
                stickyHeaderLastItem.apply {
                    if (item != null && item is MediaItem.Header) {
                        val newItem = item.text
                            .replace("Today", stringToday)
                            .replace("Yesterday", stringYesterday)
                        val newIndex = (headers.indexOf(item) - 1).coerceAtLeast(0)
                        val previousHeader = headers[newIndex].text
                            .replace("Today", stringToday)
                            .replace("Yesterday", stringYesterday)
                        value = if (firstItem != null && !firstItem.key.isHeaderKey) {
                            previousHeader
                        } else {
                            newItem
                        }
                    }
                }.value
            }
        }
        val searchBarPadding by animateDpAsState(
            targetValue = remember(isScrolling.value, showSearchBar, searchBarPaddingTop) {
                if (showSearchBar && !isScrolling.value) {
                    SearchBarDefaults.InputFieldHeight + searchBarPaddingTop + 8.dp
                } else if (showSearchBar && isScrolling.value) searchBarPaddingTop else 0.dp
            },
            label = "searchBarPadding"
        )

        val density = LocalDensity.current
        val searchBarOffset = remember(density, showSearchBar, searchBarPadding) {
            with(density) {
                return@with if (showSearchBar)
                    28.sp.roundToPx() + searchBarPadding.roundToPx() else 0
            }
        }
        val headerMatcher: (LazyGridItemInfo) -> Boolean = remember {
            { item -> item.key.isHeaderKey || item.key.isIgnoredKey }
        }
        val headerOffset by rememberHeaderOffset(gridState, headerMatcher, searchBarOffset)
        StickyHeaderGrid(
            modifier = Modifier.fillMaxSize(),
            showSearchBar = showSearchBar,
            headerOffset = headerOffset,
            stickyHeader = {
                val show = remember(
                    mediaState,
                    stickyHeaderItem
                ) { mediaState.media.isNotEmpty() && stickyHeaderItem != null }
                AnimatedVisibility(
                    visible = show,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    Text(
                        text = stickyHeaderItem.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        // 3.dp is the elevation the LargeTopAppBar use
                                        MaterialTheme.colorScheme.surfaceColorAtElevation(
                                            3.dp
                                        ),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp + searchBarPadding, bottom = 24.dp)
                            .fillMaxWidth()
                    )
                }
            }
        ) { mediaGrid() }
    } else mediaGrid()*/


}