/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.common.components

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.dot.gallery.R
import com.dot.gallery.core.MediaKey
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.presentation.components.StickyHeader
import com.dot.gallery.core.presentation.components.util.StickyHeaderGrid
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isBigHeaderKey
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.domain.model.isIgnoredKey
import com.dot.gallery.feature_node.presentation.util.clear
import com.dot.gallery.feature_node.presentation.util.update
import com.dot.gallery.feature_node.presentation.util.vibrate
import com.dot.gallery.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGridView(
    mediaState: MediaState,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    gridState: LazyGridState = rememberLazyGridState(),
    searchBarPaddingTop: Dp = 0.dp,
    showSearchBar: Boolean = false,
    allowSelection: Boolean = false,
    selectionState: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
    selectedMedia: MutableState<Set<Long>> = rememberSaveable { mutableStateOf(emptySet()) },
    toggleSelection: (Int) -> Unit = {},
    allowHeaders: Boolean = true,
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

    /** Glide Preloading **/
    val preloadingData = rememberGlidePreloadingData(
        data = mediaState.media,
        preloadImageSize = Size(24f, 24f),
        fixedVisibleItemCount = 4
    ) { media: Media, requestBuilder: RequestBuilder<Drawable> ->
        requestBuilder
            .signature(MediaKey(media.id, media.timestamp, media.mimeType, media.orientation))
            .load(media.uri)
    }
    /** ************ **/

    /** Selection state handling **/
    BackHandler(
        enabled = selectionState.value && allowSelection,
        onBack = {
            selectionState.value = false
            selectedMedia.clear()
        }
    )
    /** ************ **/

    @Composable
    fun mediaGrid() {
        LaunchedEffect(gridState.isScrollInProgress) {
            isScrolling.value = gridState.isScrollInProgress
        }
        val autoScrollSpeed = remember { mutableFloatStateOf(0f) }
        LaunchedEffect(autoScrollSpeed.floatValue) {
            if (autoScrollSpeed.floatValue != 0f) {
                while (isActive) {
                    gridState.scrollBy(autoScrollSpeed.floatValue)
                    delay(10)
                }
            }
        }
        val scrollGestureActive = remember { mutableStateOf(false) }
        Box {
            LazyVerticalGrid(
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .photoGridDragHandler(
                        lazyGridState = gridState,
                        haptics = LocalHapticFeedback.current,
                        selectedIds = selectedMedia,
                        autoScrollSpeed = autoScrollSpeed,
                        autoScrollThreshold = with(LocalDensity.current) { 40.dp.toPx() },
                        scrollGestureActive = scrollGestureActive
                    ),
                columns = GridCells.Adaptive(Dimens.Photo()),
                contentPadding = paddingValues,
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
                        key = { if (it is MediaItem.MediaViewItem) it.media.id else it.key },
                        contentType = { it.key.startsWith("media_") },
                        span = { item ->
                            GridItemSpan(if (item.key.isHeaderKey) maxLineSpan else 1)
                        }
                    ) { item ->
                        when (item) {
                            is MediaItem.Header -> {
                                val isChecked = rememberSaveable { mutableStateOf(false) }
                                if (allowSelection) {
                                    LaunchedEffect(selectionState.value) {
                                        // Uncheck if selectionState is set to false
                                        isChecked.value = isChecked.value && selectionState.value
                                    }
                                    LaunchedEffect(selectedMedia.value.size) {
                                        // Partial check of media items should not check the header
                                        isChecked.value =
                                            selectedMedia.value.containsAll(item.data.map { it.id }
                                                .toSet())
                                    }
                                }
                                val title = item.text
                                    .replace("Today", stringToday)
                                    .replace("Yesterday", stringYesterday)

                                val view = LocalView.current
                                StickyHeader(
                                    date = title,
                                    showAsBig = item.key.isBigHeaderKey,
                                    isCheckVisible = selectionState,
                                    isChecked = isChecked
                                ) {
                                    if (allowSelection) {
                                        view.vibrate()
                                        scope.launch {
                                            isChecked.value = !isChecked.value
                                            val list = item.data.map { it.id }.toSet()
                                            if (isChecked.value) {
                                                selectedMedia.value = selectedMedia.value.plus(list)
                                            } else {
                                                selectedMedia.value =
                                                    selectedMedia.value.minus(list)
                                            }
                                            selectionState.update(selectedMedia.value.isNotEmpty())
                                        }
                                    }
                                }
                            }

                            is MediaItem.MediaViewItem -> {
                                val mediaIndex =
                                    mediaState.media.indexOf(item.media).coerceAtLeast(0)
                                val (media, preloadRequestBuilder) = preloadingData[mediaIndex]
                                val view = LocalView.current
                                MediaComponent(
                                    media = media,
                                    selectionState = selectionState,
                                    selectedMedia = selectedMedia,
                                    preloadRequestBuilder = preloadRequestBuilder,
                                    scrollGestureActive = scrollGestureActive.value,
                                    onItemLongClick = {
                                        if (allowSelection) {
                                            view.vibrate()
                                            toggleSelection(mediaState.media.indexOf(it))
                                        }
                                    },
                                    onItemClick = {
                                        if (selectionState.value && allowSelection) {
                                            view.vibrate()
                                            toggleSelection(mediaState.media.indexOf(it))
                                        } else onMediaClick(it)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = mediaState.media,
                        key = { it.toString() },
                        contentType = { it.mimeType }
                    ) { origMedia ->
                        val mediaIndex = mediaState.media.indexOf(origMedia).coerceAtLeast(0)
                        val (media, preloadRequestBuilder) = preloadingData[mediaIndex]
                        val view = LocalView.current
                        MediaComponent(
                            media = media,
                            selectionState = selectionState,
                            selectedMedia = selectedMedia,
                            preloadRequestBuilder = preloadRequestBuilder,
                            onItemLongClick = {
                                if (allowSelection) {
                                    view.vibrate()
                                    toggleSelection(mediaState.media.indexOf(it))
                                }
                            },
                            onItemClick = {
                                if (selectionState.value && allowSelection) {
                                    view.vibrate()
                                    toggleSelection(mediaState.media.indexOf(it))
                                } else onMediaClick(it)
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
            }
        }
    }

    if (enableStickyHeaders) {
        /**
         * Remember last known header item
         */
        val stickyHeaderLastItem = remember { mutableStateOf<String?>(null) }

        val headers = remember(mappedData) {
            mappedData.filterIsInstance<MediaItem.Header>()
        }

        val stickyHeaderItem by remember(mappedData) {
            derivedStateOf {
                val firstItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull()
                var firstHeaderIndex =
                    gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key.isHeaderKey }?.index
                var item = firstHeaderIndex?.let(mappedData::getOrNull)
                if (item != null && item.key.isBigHeaderKey) {
                    firstHeaderIndex = firstHeaderIndex!! + 1
                    item = firstHeaderIndex.let(mappedData::getOrNull)
                }
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
            targetValue = if (showSearchBar && !isScrolling.value) {
                SearchBarDefaults.InputFieldHeight + searchBarPaddingTop + 8.dp
            } else if (showSearchBar && isScrolling.value) searchBarPaddingTop else 0.dp,
            label = "searchBarPadding"
        )
        StickyHeaderGrid(
            modifier = Modifier.fillMaxSize(),
            lazyState = gridState,
            headerMatcher = { item -> item.key.isHeaderKey || item.key.isIgnoredKey },
            showSearchBar = showSearchBar,
            searchBarPadding = searchBarPadding,
            stickyHeader = {
                if (mediaState.media.isNotEmpty()) {
                    stickyHeaderItem?.let {
                        Text(
                            text = it,
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
            },
            content = { mediaGrid() }
        )
    } else mediaGrid()


}

fun Modifier.photoGridDragHandler(
    lazyGridState: LazyGridState,
    haptics: HapticFeedback,
    selectedIds: MutableState<Set<Long>>,
    autoScrollSpeed: MutableState<Float>,
    autoScrollThreshold: Float,
    scrollGestureActive: MutableState<Boolean>
) = pointerInput(Unit) {
    fun LazyGridState.gridItemKeyAtPosition(hitPoint: Offset): Long? {
        val key = layoutInfo.visibleItemsInfo.find { itemInfo ->
            itemInfo.size.toIntRect().contains(hitPoint.round() - itemInfo.offset)
        }?.key as? Long
        return if (key?.isHeaderKey == false) key else null
    }

    var initialKey: Long? = null
    var currentKey: Long? = null
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            scrollGestureActive.value = true
            lazyGridState.gridItemKeyAtPosition(offset)?.let { key ->
                if (!selectedIds.value.contains(key)) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    initialKey = key
                    currentKey = key
                    selectedIds.value += key
                }
            }
        },
        onDragCancel = {
            scrollGestureActive.value = false
            initialKey = null
            autoScrollSpeed.value = 0f
        },
        onDragEnd = {
            scrollGestureActive.value = false
            initialKey = null
            autoScrollSpeed.value = 0f
        },
        onDrag = { change, _ ->
            if (initialKey != null) {
                val distFromBottom =
                    lazyGridState.layoutInfo.viewportSize.height - change.position.y
                val distFromTop = change.position.y
                autoScrollSpeed.value = when {
                    distFromBottom < autoScrollThreshold -> autoScrollThreshold - distFromBottom
                    distFromTop < autoScrollThreshold -> -(autoScrollThreshold - distFromTop)
                    else -> 0f
                }

                lazyGridState.gridItemKeyAtPosition(change.position)?.let { key ->
                    if (currentKey != key) {
                        selectedIds.value = selectedIds.value
                            .minus(initialKey!!..currentKey!!)
                            .minus(currentKey!!..initialKey!!)
                            .plus(initialKey!!..key)
                            .plus(key..initialKey!!)
                        currentKey = key
                    }
                }
            }
        }
    )
}
