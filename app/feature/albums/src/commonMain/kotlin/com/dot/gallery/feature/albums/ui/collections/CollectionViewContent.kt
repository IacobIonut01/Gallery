/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature.albums.ui.collections

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.cellsList
import com.dot.gallery.core.LocalMediaDistributor
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.core.presentation.components.GridPinchZoomLayout
import com.dot.gallery.core.presentation.components.MediaGridView
import com.dot.gallery.core.presentation.components.MosaicMediaGrid
import com.dot.gallery.core.presentation.components.MosaicPinchZoomLayout
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.TimelineScroller
import com.dot.gallery.core.presentation.components.TwoLinedDateToolbarTitle
import com.dot.gallery.core.presentation.components.rememberGridPinchZoomState
import com.dot.gallery.core.presentation.components.rememberMosaicMonthSegments
import com.dot.gallery.core.presentation.components.rememberMosaicPinchZoomState
import com.dot.gallery.core.presentation.util.LocalHazeState
import com.dot.gallery.core.presentation.util.hazeEffectScaled
import com.dot.gallery.core.presentation.util.selectedMedia
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.settings.GROUP_MONTHLY
import com.dot.gallery.domain.settings.GROUP_NORMAL
import com.dot.gallery.domain.settings.GROUP_YEARLY
import com.dot.gallery.domain.settings.LAYOUT_MOSAIC
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.feature.albums.ui.components.AlbumSortDropdown
import dev.chrisbanes.haze.blur.LocalHazeBlurStyle
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.albums.generated.resources.Res
import com.dot.gallery.feature.albums.generated.resources.collection_empty
import com.dot.gallery.feature.albums.generated.resources.edit

/**
 * Pure-Compose port of the collection media grid.
 *
 * Platform seams:
 * - [metadataState] is provided app-side (`distributor.metadataFlow`).
 * - [gridSizeIndex]/[mosaicGridSizeIndex] are the persisted grid/mosaic column
 *   indexes (the app keeps the orientation-aware SharedPreferences state).
 * - [onMediaClick] receives the tapped media; the app owns navigation to
 *   `Screen.MediaViewScreen.idAndCollection`.
 * - [selectionSheet] hosts the app's `SelectionSheet` overlay (move/copy/share
 *   actions stay app-side); it is placed in the root [Box] with
 *   `Alignment.BottomEnd` semantics left to the caller's modifier.
 */
@OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun CollectionViewContent(
    collectionId: Long,
    collectionName: String,
    paddingValues: PaddingValues,
    isScrolling: MutableState<Boolean>,
    metadataState: State<MediaMetadataState>,
    gridSizeIndex: MutableState<Int>,
    mosaicGridSizeIndex: MutableState<Int>,
    onMediaClick: (UriMedia) -> Unit,
    onEditAlbums: (() -> Unit)? = null,
    selectionSheet: @Composable BoxScope.(mediaState: MediaState<UriMedia>, selectedMedia: SnapshotStateList<UriMedia>) -> Unit = { _, _ -> },
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    var canScroll by rememberSaveable { mutableStateOf(true) }
    var lastCellIndex by gridSizeIndex
    val distributor = LocalMediaDistributor.current
    val selector = LocalMediaSelector.current
    val selectedMedia = selector.selectedMedia.collectAsState()

    val collectionMediaFlow = remember(collectionId) {
        distributor.collectionMediaFlow(collectionId)
    }
    val mediaState = collectionMediaFlow.collectAsState()

    var albumMediaSort by rememberPreference(SettingsKeys.albumMediaSort)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { canScroll },
        flingAnimationSpec = null
    )

    val cacheWindow = remember { LazyLayoutCacheWindow(aheadFraction = 2f, behindFraction = 2f) }
    val pinchState = rememberGridPinchZoomState(
        cellsList = cellsList,
        initialCellsIndex = lastCellIndex,
        gridState = rememberLazyGridState(cacheWindow = cacheWindow)
    )

    LaunchedEffect(pinchState.isZooming) {
        withContext(Dispatchers.Default) {
            canScroll = !pinchState.isZooming
            lastCellIndex = cellsList.indexOf(pinchState.currentCells)
        }
    }

    Box(
        modifier = Modifier
            .padding(
                start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
            )
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    modifier = Modifier.hazeEffectScaled(
                        state = LocalHazeState.current,
                        style = LocalHazeBlurStyle.current
                    ),
                    title = {
                        TwoLinedDateToolbarTitle(
                            albumName = collectionName,
                            dateHeader = mediaState.value.dateHeader
                        )
                    },
                    navigationIcon = {
                        NavigationBackButton()
                    },
                    actions = {
                        if (onEditAlbums != null) {
                            IconButton(onClick = onEditAlbums) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(Res.string.edit)
                                )
                            }
                        }
                        AlbumSortDropdown(
                            currentSort = albumMediaSort,
                            onSortChange = { newSort ->
                                albumMediaSort = newSort
                            }
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        ) { innerPadding ->
            val hideTimelineOnAlbum by rememberPreference(SettingsKeys.hideTimelineOnAlbum)
            val albumsGroupMethod by rememberPreference(SettingsKeys.albumsGroupMethod)
            val timelineLayoutType by rememberPreference(SettingsKeys.timelineLayoutType)
            val isMosaicLayout = timelineLayoutType == LAYOUT_MOSAIC && !hideTimelineOnAlbum
            if (isMosaicLayout) {
                var lastMosaicCellIndex by mosaicGridSizeIndex
                val mosaicPinchState = rememberMosaicPinchZoomState(
                    initialColumnsIndex = lastMosaicCellIndex,
                    gridState = rememberLazyGridState(cacheWindow = cacheWindow)
                )
                val mosaicGridState = mosaicPinchState.gridState

                LaunchedEffect(mosaicPinchState.isZooming) {
                    lastMosaicCellIndex = mosaicPinchState.currentColumnsIndex
                }

                val mappedData by remember(mediaState, albumsGroupMethod) {
                    derivedStateOf {
                        when (albumsGroupMethod) {
                            GROUP_MONTHLY -> mediaState.value.mappedMediaWithMonthly
                            GROUP_YEARLY -> mediaState.value.mappedMediaWithYearly
                            else -> mediaState.value.mappedMedia
                        }.toMutableStateList()
                    }
                }
                val headers by remember(mediaState) {
                    derivedStateOf {
                        mediaState.value.headers.toMutableStateList()
                    }
                }
                val mosaicPaddingValues = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 128.dp
                )
                MosaicPinchZoomLayout(
                    state = mosaicPinchState,
                    indicatorTopPadding = mosaicPaddingValues.calculateTopPadding() + 16.dp,
                ) { currentColumns ->
                    TimelineScroller(
                        modifier = Modifier
                            .padding(mosaicPaddingValues)
                            .padding(top = 32.dp)
                            .padding(vertical = 32.dp),
                        segments = rememberMosaicMonthSegments(
                            mappedData = mappedData,
                            columns = currentColumns,
                            allowHeaders = true,
                            leadingItemCount = 0,
                        ),
                        headers = headers,
                        state = mosaicGridState,
                    ) {
                        MosaicMediaGrid(
                            modifier = Modifier.hazeSource(LocalHazeState.current),
                            gridState = mosaicGridState,
                            columns = currentColumns,
                            mediaState = mediaState,
                            metadataState = metadataState,
                            mappedData = mappedData,
                            paddingValues = mosaicPaddingValues,
                            allowSelection = true,
                            canScroll = !mosaicPinchState.isZooming,
                            allowHeaders = true,
                            aboveGridContent = null,
                            isScrolling = isScrolling,
                            emptyContent = {
                                EmptyMedia(
                                    title = stringResource(Res.string.collection_empty)
                                )
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            onMediaClick = onMediaClick,
                        )
                    }
                }
            } else {
                GridPinchZoomLayout(
                    state = pinchState,
                    modifier = Modifier.hazeSource(LocalHazeState.current),
                    indicatorTopPadding = innerPadding.calculateTopPadding() + 16.dp,
                ) {
                    MediaGridView(
                        mediaState = mediaState,
                        metadataState = metadataState,
                        allowSelection = true,
                        showSearchBar = false,
                        enableStickyHeaders = !hideTimelineOnAlbum,
                        groupMethod = if (!hideTimelineOnAlbum) albumsGroupMethod else GROUP_NORMAL,
                        paddingValues = PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding() + 128.dp
                        ),
                        canScroll = canScroll,
                        allowHeaders = !hideTimelineOnAlbum,
                        aboveGridContent = null,
                        isScrolling = isScrolling,
                        emptyContent = {
                            EmptyMedia(
                                title = stringResource(Res.string.collection_empty)
                            )
                        },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        onMediaClick = onMediaClick,
                    )
                }
            }
        }
        val selectedMediaList by selectedMedia(
            media = mediaState.value.media,
            selectedSet = selectedMedia
        )
        selectionSheet(mediaState.value, selectedMediaList)
    }
}
