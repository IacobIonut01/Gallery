/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.timeline

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.MediaScreen
import com.dot.gallery.feature_node.presentation.timeline.components.TimelineNavActions

@Composable
fun TimelineScreen(
    paddingValues: PaddingValues,
    albumId: Long = -1L,
    albumName: String = stringResource(R.string.app_name),
    retrieveMedia: (() -> Unit)? = null,
    handler: MediaHandleUseCase,
    mediaState: MutableState<MediaState>,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<Media>,
    toggleSelection: (Int) -> Unit,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit
) = MediaScreen(
    paddingValues = paddingValues,
    albumId = albumId,
    target = null,
    albumName = albumName,
    retrieveMedia = retrieveMedia,
    mediaState = mediaState,
    selectionState = selectionState,
    selectedMedia = selectedMedia,
    toggleSelection = toggleSelection,
    showMonthlyHeader = true,
    alwaysGoBack = false,
    NavActions = {
                   expandedDropDown: MutableState<Boolean>,
                   result: ActivityResultLauncher<IntentSenderRequest> ->
        TimelineNavActions(
            albumId = albumId,
            handler = handler,
            expandedDropDown = expandedDropDown,
            mediaState = mediaState,
            selectedMedia = selectedMedia,
            selectionState = selectionState,
            navigate = navigate,
            navigateUp = navigateUp,
            result = result
        )
    },
    EmptyComponent = { EmptyMedia(Modifier.fillMaxSize()) },
    navigate = navigate,
    navigateUp = navigateUp
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        selectedMedia.clear()
        selectionState.value = false
    }
}