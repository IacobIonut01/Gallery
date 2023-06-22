/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.trashed

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
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.common.MediaScreen
import com.dot.gallery.feature_node.presentation.trashed.components.AutoDeleteFooter
import com.dot.gallery.feature_node.presentation.trashed.components.EmptyTrash
import com.dot.gallery.feature_node.presentation.trashed.components.TrashedNavActions
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TrashedGridScreen(
    paddingValues: PaddingValues,
    albumName: String = stringResource(id = R.string.trash),
    handler: MediaHandleUseCase,
    mediaState: StateFlow<MediaState>,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<Media>,
    toggleSelection: (Int) -> Unit,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit,
    toggleNavbar: (Boolean) -> Unit
) = MediaScreen(
    paddingValues = paddingValues,
    target = TARGET_TRASH,
    albumName = albumName,
    handler = handler,
    mediaState = mediaState,
    selectionState = selectionState,
    selectedMedia = selectedMedia,
    toggleSelection = toggleSelection,
    navActionsContent = { _: MutableState<Boolean>,
                          _: ActivityResultLauncher<IntentSenderRequest> ->
        TrashedNavActions(handler, mediaState, selectedMedia, selectionState)
    },
    emptyContent = { EmptyTrash(Modifier.fillMaxSize()) },
    aboveGridContent = { AutoDeleteFooter() },
    enableStickyHeaders = false,
    navigate = navigate,
    navigateUp = navigateUp,
    toggleNavbar = toggleNavbar
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        selectedMedia.clear()
        selectionState.value = false
    }
}