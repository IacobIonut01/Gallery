/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.timeline

import android.app.Activity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.common.MediaScreen
import com.dot.gallery.feature_node.presentation.timeline.components.RequestMediaManager
import com.dot.gallery.feature_node.presentation.timeline.components.TimelineNavActions
import com.dot.gallery.feature_node.presentation.util.clear
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TimelineScreen(
    paddingValues: PaddingValues,
    albumId: Long = -1L,
    albumName: String = stringResource(R.string.app_name),
    handler: MediaHandleUseCase,
    mediaState: StateFlow<MediaState>,
    selectionState: MutableState<Boolean>,
    selectedMedia: MutableState<Set<Long>>,
    allowNavBar: Boolean = true,
    allowHeaders: Boolean = true,
    enableStickyHeaders: Boolean = true,
    toggleSelection: (Int) -> Unit,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit,
    toggleNavbar: (Boolean) -> Unit,
    isScrolling: MutableState<Boolean>,
    searchBarActive: MutableState<Boolean> = mutableStateOf(false)
) {
    val useMediaManager by Settings.Misc.rememberIsMediaManager()
    val aboveGrid: @Composable (() -> Unit)? =
        if (!useMediaManager) {
            { RequestMediaManager() }
        } else null
    MediaScreen(
        paddingValues = paddingValues,
        albumId = albumId,
        target = null,
        albumName = albumName,
        handler = handler,
        mediaState = mediaState,
        selectionState = selectionState,
        selectedIds = selectedMedia,
        toggleSelection = toggleSelection,
        allowHeaders = allowHeaders,
        enableStickyHeaders = enableStickyHeaders,
        showMonthlyHeader = true,
        allowNavBar = allowNavBar,
        navActionsContent = { expandedDropDown: MutableState<Boolean>, _ ->
            TimelineNavActions(
                albumId = albumId,
                handler = handler,
                expandedDropDown = expandedDropDown,
                mediaState = mediaState,
                selectedMedia = selectedMedia,
                selectionState = selectionState,
                navigate = navigate,
                navigateUp = navigateUp
            )
        },
        emptyContent = { EmptyMedia(Modifier.fillMaxSize()) },
        aboveGridContent = aboveGrid,
        navigate = navigate,
        navigateUp = navigateUp,
        toggleNavbar = toggleNavbar,
        isScrolling = isScrolling,
        searchBarActive = searchBarActive
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedMedia.clear()
            selectionState.value = false
        }
    }
}