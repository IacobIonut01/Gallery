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
import androidx.navigation.NavController
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.feature_node.presentation.MediaScreen
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.MediaViewModel
import com.dot.gallery.feature_node.presentation.timeline.components.TimelineNavActions

@Composable
fun TimelineScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    albumId: Long = -1L,
    albumName: String = stringResource(R.string.app_name),
    viewModel: MediaViewModel,
) = MediaScreen(
    navController = navController,
    paddingValues = paddingValues,
    albumId = albumId,
    albumName = albumName,
    viewModel = viewModel,
    showMonthlyHeader = true,
    alwaysGoBack = false,
    NavActions = {
                   expandedDropDown: MutableState<Boolean>,
                   selectedMedia: SnapshotStateList<Media>,
                   selectionState: MutableState<Boolean>,
                   result: ActivityResultLauncher<IntentSenderRequest> ->
        TimelineNavActions(
            viewModel,
            expandedDropDown,
            selectedMedia,
            selectionState,
            navController,
            result
        )
    },
    EmptyComponent = { EmptyMedia(Modifier.fillMaxSize()) },
    onActivityResult = { selectedMedia, selectionState, result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedMedia.clear()
            selectionState.value = false
        }
    }
)