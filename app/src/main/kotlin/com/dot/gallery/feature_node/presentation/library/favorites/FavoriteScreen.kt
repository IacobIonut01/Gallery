/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.library.favorites

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
import com.dot.gallery.core.Constants.Target.TARGET_FAVORITES
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.MediaScreen
import com.dot.gallery.feature_node.presentation.library.favorites.components.EmptyFavorites
import com.dot.gallery.feature_node.presentation.library.favorites.components.FavoriteNavActions

@Composable
fun FavoriteScreen(
    paddingValues: PaddingValues,
    albumName: String = stringResource(id = R.string.favorites),
    handler: MediaHandleUseCase,
    mediaState: MutableState<MediaState>,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<Media>,
    toggleFavorite: (ActivityResultLauncher<IntentSenderRequest>, List<Media>, Boolean) -> Unit,
    toggleSelection: (Int) -> Unit,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit,
    toggleNavbar: (Boolean) -> Unit
) = MediaScreen(
    paddingValues = paddingValues,
    target = TARGET_FAVORITES,
    albumName = albumName,
    handler = handler,
    mediaState = mediaState,
    selectionState = selectionState,
    selectedMedia = selectedMedia,
    toggleSelection = toggleSelection,
    NavActions = { _: MutableState<Boolean>,
                   result: ActivityResultLauncher<IntentSenderRequest> ->
        FavoriteNavActions(toggleFavorite, mediaState, selectedMedia, selectionState, result)
    },
    EmptyComponent = { EmptyFavorites(Modifier.fillMaxSize()) },
    navigate = navigate,
    navigateUp = navigateUp,
    toggleNavbar = toggleNavbar
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        selectedMedia.clear()
        selectionState.value = false
    }
}