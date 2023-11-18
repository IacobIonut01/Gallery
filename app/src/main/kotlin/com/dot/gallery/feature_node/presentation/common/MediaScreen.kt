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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.LoadingMedia
import com.dot.gallery.core.presentation.components.NavigationActions
import com.dot.gallery.core.presentation.components.NavigationButton
import com.dot.gallery.core.presentation.components.SelectionSheet
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.common.components.MediaGridView
import com.dot.gallery.feature_node.presentation.common.components.TwoLinedDateToolbarTitle
import com.dot.gallery.feature_node.presentation.search.MainSearchBar
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.clear
import com.dot.gallery.feature_node.presentation.util.selectedMedia
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
    handler: MediaHandleUseCase,
    mediaState: StateFlow<MediaState>,
    selectionState: MutableState<Boolean>,
    selectedIds: MutableState<Set<Long>>,
    toggleSelection: (Int) -> Unit,
    allowHeaders: Boolean = true,
    showMonthlyHeader: Boolean = false,
    enableStickyHeaders: Boolean = true,
    allowNavBar: Boolean = false,
    navActionsContent: @Composable() (RowScope.(expandedDropDown: MutableState<Boolean>, result: ActivityResultLauncher<IntentSenderRequest>) -> Unit),
    emptyContent: @Composable () -> Unit,
    aboveGridContent: @Composable() (() -> Unit)? = null,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit,
    toggleNavbar: (Boolean) -> Unit,
    isScrolling: MutableState<Boolean> = remember { mutableStateOf(false) },
    searchBarActive: MutableState<Boolean> = mutableStateOf(false),
    onActivityResult: (result: ActivityResult) -> Unit,
) {
    val showSearchBar = remember { albumId == -1L && target == null }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    /** STATES BLOCK **/
    val state by mediaState.collectAsStateWithLifecycle()
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
                                    selectedIds.clear()
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
                        bottomPadding = paddingValues.calculateBottomPadding(),
                        navigate = navigate,
                        toggleNavbar = toggleNavbar,
                        selectionState = selectionState,
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
                selectionState = selectionState,
                selectedMedia = selectedIds,
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
            if (state.error.isNotEmpty())
                Error(errorMessage = state.error)
            else if (!state.isLoading && state.media.isEmpty())
                emptyContent.invoke()
            /** ************ **/
        }
        if (target != TARGET_TRASH) {
            val selectedMediaList = state.media.selectedMedia(selectedIds)
            SelectionSheet(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                selectedMedia = selectedMediaList,
                target = target,
                selectionState = selectionState,
                handler = handler
            )
        }
    }
}