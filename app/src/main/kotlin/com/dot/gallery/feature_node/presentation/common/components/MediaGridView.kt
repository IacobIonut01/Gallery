/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.common.components

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.dot.gallery.feature_node.presentation.util.update
import com.dot.gallery.feature_node.presentation.util.vibrate
import com.dot.gallery.ui.theme.Dimens
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
    selectionState: MutableState<Boolean> = mutableStateOf(false),
    selectedMedia: SnapshotStateList<Media> = mutableStateListOf(),
    toggleSelection: (Int) -> Unit = {},
    enableStickyHeaders: Boolean = false,
    showMonthlyHeader: Boolean = false,
    aboveGridContent: @Composable() (() -> Unit)? = null,
    isScrolling: MutableState<Boolean>,
    onMediaClick: (media: Media) -> Unit = {}
) {
    val stringToday = stringResource(id = R.string.header_today)
    val stringYesterday = stringResource(id = R.string.header_yesterday)

    val scope = rememberCoroutineScope()
    val mappedData =
        if (showMonthlyHeader) mediaState.mappedMediaWithMonthly else mediaState.mappedMedia

    /** Glide Preloading **/
    val preloadingData = rememberGlidePreloadingData(
        data = mediaState.media,
        preloadImageSize = Size(48f, 48f)
    ) { media: Media, requestBuilder: RequestBuilder<Drawable> ->
        requestBuilder
            .signature(MediaKey(media.id, media.timestamp, media.mimeType, media.orientation))
            .load(media.uri)
    }
    /** ************ **/

    /** Selection state handling **/
    fun clearSelection() {
        selectionState.value = false
        selectedMedia.clear()
    }

    BackHandler(
        enabled = selectionState.value && allowSelection,
        onBack = { clearSelection() }
    )

    @Composable
    fun mediaGrid() {
        LaunchedEffect(gridState.isScrollInProgress) {
            isScrolling.value = gridState.isScrollInProgress
        }
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize(),
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

            items(
                items = mappedData,
                key = { if (it is MediaItem.MediaViewItem) it.media.toString() else it.key },
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
                            LaunchedEffect(selectedMedia.size) {
                                // Partial check of media items should not check the header
                                isChecked.value = selectedMedia.containsAll(item.data)
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
                    }

                    is MediaItem.MediaViewItem -> {
                        val mediaIndex = mediaState.media.indexOf(item.media).coerceAtLeast(0)
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
        }
    }

    if (enableStickyHeaders) {
        /**
         * Remember last known header item
         */
        val stickyHeaderLastItem = remember { mutableStateOf<String?>(null) }

        val stickyHeaderItem by remember(mediaState.media) {
            derivedStateOf {
                var firstIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                var item = firstIndex?.let(mappedData::getOrNull)
                if (item != null && item.key.contains("big")) {
                    firstIndex = firstIndex!! + 1
                    item = firstIndex.let(mappedData::getOrNull)
                }
                stickyHeaderLastItem.apply {
                    if (item != null && item is MediaItem.Header) {
                        value = item.text
                            .replace("Today", stringToday)
                            .replace("Yesterday", stringYesterday)
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