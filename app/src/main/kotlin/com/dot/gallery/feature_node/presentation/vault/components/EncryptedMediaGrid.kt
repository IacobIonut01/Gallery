package com.dot.gallery.feature_node.presentation.vault.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dokar.pinchzoomgrid.PinchZoomGridScope
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.LoadingMedia
import com.dot.gallery.core.presentation.components.MediaItemHeader
import com.dot.gallery.feature_node.domain.model.DecryptedMedia
import com.dot.gallery.feature_node.domain.model.EncryptedMediaItem
import com.dot.gallery.feature_node.domain.model.EncryptedMediaState
import com.dot.gallery.feature_node.domain.model.isBigHeaderKey
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.presentation.util.rememberFeedbackManager
import com.dot.gallery.feature_node.presentation.util.update
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun PinchZoomGridScope.EncryptedMediaGrid(
    gridState: LazyGridState,
    gridCells: GridCells,
    mediaState: State<EncryptedMediaState>,
    mappedData: SnapshotStateList<EncryptedMediaItem>,
    paddingValues: PaddingValues,
    allowSelection: Boolean,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<DecryptedMedia>,
    toggleSelection: @DisallowComposableCalls (Int) -> Unit,
    canScroll: Boolean,
    allowHeaders: Boolean,
    aboveGridContent: @Composable (() -> Unit)?,
    isScrolling: MutableState<Boolean>,
    emptyContent: @Composable () -> Unit,
    onMediaClick: @DisallowComposableCalls (media: DecryptedMedia) -> Unit
) {
    LaunchedEffect(gridState.isScrollInProgress) {
        snapshotFlow {
            gridState.isScrollInProgress
        }.collectLatest {
            isScrolling.value = it
        }
    }

    val topContent: LazyGridScope.() -> Unit = remember(aboveGridContent) {
        {
            if (aboveGridContent != null) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "aboveGrid"
                ) {
                    aboveGridContent.invoke()
                }
            }
        }
    }
    val bottomContent: LazyGridScope.() -> Unit = remember {
        {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "loading"
            ) {
                AnimatedVisibility(
                    visible = mediaState.value.isLoading,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    LoadingMedia(
                        topContent = {
                            Text(
                                text = "Decrypting media...",
                                modifier = Modifier.padding(top = 16.dp)
                                    .fillMaxWidth()
                                    .shimmer(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )
                }
            }

            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "empty"
            ) {
                AnimatedVisibility(
                    visible = mediaState.value.media.isEmpty() && !mediaState.value.isLoading,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    emptyContent()
                }
            }
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "error"
            ) {
                AnimatedVisibility(visible = mediaState.value.error.isNotEmpty()) {
                    Error(errorMessage = mediaState.value.error)
                }
            }
        }
    }

    AnimatedVisibility(
        visible = allowHeaders
    ) {
        EncryptedMediaGridContentWithHeaders(
            gridCells = gridCells,
            mediaState = mediaState,
            mappedData = mappedData,
            paddingValues = paddingValues,
            allowSelection = allowSelection,
            selectionState = selectionState,
            selectedMedia = selectedMedia,
            toggleSelection = toggleSelection,
            canScroll = canScroll,
            onMediaClick = onMediaClick,
            topContent = topContent,
            bottomContent = bottomContent
        )
    }

    AnimatedVisibility(
        visible = !allowHeaders
    ) {
        EncryptedMediaGridContent(
            gridCells = gridCells,
            mediaState = mediaState,
            paddingValues = paddingValues,
            allowSelection = allowSelection,
            selectionState = selectionState,
            selectedMedia = selectedMedia,
            toggleSelection = toggleSelection,
            canScroll = canScroll,
            onMediaClick = onMediaClick,
            topContent = topContent,
            bottomContent = bottomContent
        )
    }

}

@Composable
private fun PinchZoomGridScope.EncryptedMediaGridContentWithHeaders(
    gridCells: GridCells,
    mediaState: State<EncryptedMediaState>,
    mappedData: SnapshotStateList<EncryptedMediaItem>,
    paddingValues: PaddingValues,
    allowSelection: Boolean,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<DecryptedMedia>,
    toggleSelection: @DisallowComposableCalls (Int) -> Unit,
    canScroll: Boolean,
    onMediaClick: @DisallowComposableCalls (media: DecryptedMedia) -> Unit,
    topContent: LazyGridScope.() -> Unit,
    bottomContent: LazyGridScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val stringToday = stringResource(id = R.string.header_today)
    val stringYesterday = stringResource(id = R.string.header_yesterday)
    val feedbackManager = rememberFeedbackManager()
    EncryptedTimelineScroller(
        modifier = Modifier
            .padding(paddingValues)
            .padding(top = 32.dp)
            .padding(vertical = 32.dp),
        mappedData = mappedData,
        headers = remember(mediaState.value) {
            mediaState.value.headers.toMutableStateList()
        },
        state = gridState,
    ) {
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            columns = gridCells,
            contentPadding = paddingValues,
            userScrollEnabled = canScroll,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            topContent()

            items(
                items = mappedData,
                key = { item -> item.key },
                contentType = { item -> item.key.startsWith("media_") },
                span = { item ->
                    GridItemSpan(if (item.key.isHeaderKey) maxLineSpan else 1)
                }
            ) { it ->
                if (it is EncryptedMediaItem.Header) {
                    val isChecked = rememberSaveable { mutableStateOf(false) }
                    if (allowSelection) {
                        LaunchedEffect(selectionState.value) {
                            // Uncheck if selectionState is set to false
                            isChecked.value = isChecked.value && selectionState.value
                        }
                        LaunchedEffect(selectedMedia.size) {
                            // Partial check of media items should not check the header
                            isChecked.value = selectedMedia.map { it.id }.containsAll(it.data)
                        }
                    }
                    MediaItemHeader(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null
                            )
                            .pinchItem(key = it.key),
                        date = remember {
                            it.text
                                .replace("Today", stringToday)
                                .replace("Yesterday", stringYesterday)
                        },
                        showAsBig = remember { it.key.isBigHeaderKey },
                        isCheckVisible = selectionState,
                        isChecked = isChecked
                    ) {
                        if (allowSelection) {
                            feedbackManager.vibrate()
                            scope.launch {
                                isChecked.value = !isChecked.value
                                if (isChecked.value) {
                                    val toAdd = it.data.toMutableList().apply {
                                        // Avoid media from being added twice to selection
                                        removeIf {
                                            selectedMedia.map { media -> media.id }.contains(it)
                                        }
                                    }
                                    selectedMedia.addAll(mediaState.value.media.filter {
                                        toAdd.contains(
                                            it.id
                                        )
                                    })
                                } else selectedMedia.removeAll { media -> it.data.contains(media.id) }
                                selectionState.update(selectedMedia.isNotEmpty())
                            }
                        }
                    }
                } else if (it is EncryptedMediaItem.MediaViewItem) {
                    EncryptedMediaImage(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null
                            )
                            .pinchItem(key = it.key),
                        media = it.media,
                        selectionState = selectionState,
                        selectedMedia = selectedMedia,
                        canClick = canScroll,
                        onItemClick = {
                            if (selectionState.value && allowSelection) {
                                feedbackManager.vibrate()
                                toggleSelection(mediaState.value.media.indexOf(it))
                            } else onMediaClick(it)
                        }
                    ) {
                        if (allowSelection) {
                            feedbackManager.vibrate()
                            toggleSelection(mediaState.value.media.indexOf(it))
                        }
                    }
                }
            }


            bottomContent()
        }
    }

    /*TimelineScroller2(
        gridState = gridState,
        mappedData = mappedData,
        headerList = remember(mediaState.value) {
            mediaState.value.headers.toMutableStateList()
        },
        paddingValues = paddingValues
    )*/
}

@Composable
private fun PinchZoomGridScope.EncryptedMediaGridContent(
    gridCells: GridCells,
    mediaState: State<EncryptedMediaState>,
    paddingValues: PaddingValues,
    allowSelection: Boolean,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<DecryptedMedia>,
    toggleSelection: @DisallowComposableCalls (Int) -> Unit,
    canScroll: Boolean,
    onMediaClick: @DisallowComposableCalls (media: DecryptedMedia) -> Unit,
    topContent: LazyGridScope.() -> Unit,
    bottomContent: LazyGridScope.() -> Unit
) {
    val feedbackManager = rememberFeedbackManager()
    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        columns = gridCells,
        contentPadding = paddingValues,
        userScrollEnabled = canScroll,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        topContent()

        itemsIndexed(
            items = mediaState.value.media,
            key = { _, item -> item.toString() },
            contentType = { _, item -> item.isImage }
        ) { index, media ->
            EncryptedMediaImage(
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = null
                    )
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

        bottomContent()
    }
}