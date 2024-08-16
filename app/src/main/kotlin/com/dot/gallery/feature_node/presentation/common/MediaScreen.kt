/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.common

import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState
import com.dot.gallery.core.AlbumState
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.Constants.cellsList
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.LoadingMedia
import com.dot.gallery.core.presentation.components.NavigationActions
import com.dot.gallery.core.presentation.components.NavigationButton
import com.dot.gallery.core.presentation.components.SelectionSheet
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.common.components.MediaGridView
import com.dot.gallery.feature_node.presentation.common.components.TwoLinedDateToolbarTitle
import com.dot.gallery.feature_node.presentation.search.MainSearchBar
import com.dot.gallery.feature_node.presentation.util.Screen
import kotlinx.coroutines.flow.StateFlow

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun MediaScreen(
    paddingValues: PaddingValues,
    albumId: Long = -1L,
    target: String? = null,
    albumName: String,
    vm: MediaViewModel,
    handler: MediaHandleUseCase,
    albumState: StateFlow<AlbumState>,
    mediaState: StateFlow<MediaState>,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<Media>,
    toggleSelection: (Int) -> Unit,
    allowHeaders: Boolean = true,
    showMonthlyHeader: Boolean = false,
    enableStickyHeaders: Boolean = true,
    allowNavBar: Boolean = false,
    navActionsContent: @Composable() (RowScope.(expandedDropDown: MutableState<Boolean>, result: ActivityResultLauncher<IntentSenderRequest>) -> Unit),
    emptyContent: @Composable (PaddingValues) -> Unit,
    aboveGridContent: @Composable() (() -> Unit)? = null,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit,
    toggleNavbar: (Boolean) -> Unit,
    isScrolling: MutableState<Boolean> = remember { mutableStateOf(false) },
    searchBarActive: MutableState<Boolean> = mutableStateOf(false),
    onActivityResult: (result: ActivityResult) -> Unit,
) {
    val showSearchBar = remember { albumId == -1L && target == null }
    var canScroll by rememberSaveable { mutableStateOf(true) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { canScroll },
        flingAnimationSpec = null
    )
    var lastCellIndex by rememberGridSize()

    val pinchState = rememberPinchZoomGridState(
        cellsList = cellsList,
        initialCellsIndex = lastCellIndex
    )

    LaunchedEffect(pinchState.isZooming) {
        canScroll = !pinchState.isZooming
        lastCellIndex = cellsList.indexOf(pinchState.currentCells)
    }

    /** STATES BLOCK **/
    val state by mediaState.collectAsStateWithLifecycle()
    val albumsState by albumState.collectAsStateWithLifecycle()
    /** ************ **/

    /** Selection state handling **/
    LaunchedEffect(LocalConfiguration.current, selectionState.value) {
        if (allowNavBar) {
            toggleNavbar(!selectionState.value)
        }
    }
    /** ************  **/
    Box {
        Scaffold(
            modifier = Modifier
                .then(
                    if (!showSearchBar)
                        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    else Modifier
                ),
            topBar = {
                if (!showSearchBar) {
                    LargeTopAppBar(
                        title = {
                            TwoLinedDateToolbarTitle(
                                albumName = albumName,
                                dateHeader = state.dateHeader
                            )
                        },
                        navigationIcon = {
                            NavigationButton(
                                albumId = albumId,
                                target = target,
                                navigateUp = navigateUp,
                                clearSelection = {
                                    selectionState.value = false
                                    selectedMedia.clear()
                                },
                                selectionState = selectionState,
                                alwaysGoBack = true,
                            )
                        },
                        actions = {
                            NavigationActions(
                                actions = navActionsContent,
                                onActivityResult = onActivityResult
                            )
                        },
                        scrollBehavior = scrollBehavior
                    )
                } else {
                    MainSearchBar(
                        mediaViewModel = vm,
                        bottomPadding = paddingValues.calculateBottomPadding(),
                        navigate = navigate,
                        toggleNavbar = toggleNavbar,
                        selectionState = if (selectedMedia.isNotEmpty()) selectionState else null,
                        isScrolling = isScrolling,
                        activeState = searchBarActive
                    ) {
                        NavigationActions(
                            actions = navActionsContent,
                            onActivityResult = onActivityResult
                        )
                    }
                }
            }
        ) { it ->
            PinchZoomGridLayout(state = pinchState) {
                AnimatedVisibility(
                    visible = state.isLoading,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    LoadingMedia(
                        paddingValues = PaddingValues(
                            top = it.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding() + 16.dp + 64.dp
                        )
                    )
                }
                MediaGridView(
                    mediaState = state,
                    allowSelection = true,
                    showSearchBar = showSearchBar,
                    searchBarPaddingTop = paddingValues.calculateTopPadding(),
                    enableStickyHeaders = enableStickyHeaders,
                    paddingValues = PaddingValues(
                        top = it.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 16.dp + 64.dp
                    ),
                    canScroll = canScroll,
                    selectionState = selectionState,
                    selectedMedia = selectedMedia,
                    allowHeaders = allowHeaders,
                    showMonthlyHeader = showMonthlyHeader,
                    toggleSelection = toggleSelection,
                    aboveGridContent = aboveGridContent,
                    isScrolling = isScrolling
                ) {
                    val albumRoute = "albumId=$albumId"
                    val targetRoute = "target=$target"
                    val param =
                        if (target != null) targetRoute else albumRoute
                    navigate(Screen.MediaViewScreen.route + "?mediaId=${it.id}&$param")
                }
                /** Error State Handling Block **/
                val showError = remember(state) { state.error.isNotEmpty() }
                AnimatedVisibility(visible = showError) {
                    Error(errorMessage = state.error)
                }
                val showEmpty = remember(state) { state.media.isEmpty() && !state.isLoading && !showError }
                AnimatedVisibility(visible = showEmpty) {
                    emptyContent.invoke(PaddingValues(
                        top = it.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 16.dp + 64.dp
                    ))
                }
                /** ************ **/
            }
        }
        if (target != TARGET_TRASH) {
            SelectionSheet(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                target = target,
                selectedMedia = selectedMedia,
                selectionState = selectionState,
                albumsState = albumsState,
                handler = handler
            )
        }
    }
}