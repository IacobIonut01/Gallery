/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.timeline.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.util.Screen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun TimelineNavActions(
    albumId: Long,
    handler: MediaHandleUseCase,
    expandedDropDown: MutableState<Boolean>,
    mediaState: StateFlow<MediaState>,
    selectedMedia: SnapshotStateList<Media>,
    selectionState: MutableState<Boolean>,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit
) {
    fun clearSelection() {
        selectedMedia.clear()
        selectionState.value = false
    }
    val state by mediaState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                clearSelection()
            }
        }
    )
    IconButton(onClick = { expandedDropDown.value = !expandedDropDown.value }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.drop_down_cd)
        )
    }
    DropdownMenu(
        modifier = Modifier,
        expanded = expandedDropDown.value,
        onDismissRequest = { expandedDropDown.value = false }
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = if (selectionState.value)
                        stringResource(R.string.unselect_all)
                    else
                        stringResource(R.string.select_all)
                )
            },
            onClick = {
                selectionState.value = !selectionState.value
                if (selectionState.value)
                    selectedMedia.addAll(state.media)
                else
                    selectedMedia.clear()
                expandedDropDown.value = false
            },
        )
        if (albumId != -1L) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.move_album_to_trash)) },
                enabled = !selectionState.value,
                onClick = {
                    scope.launch {
                        handler.trashMedia(
                            result = result,
                            mediaList = state.media,
                            trash = true
                        )
                        navigateUp()
                    }
                    expandedDropDown.value = false
                },
            )
        }
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.favorites)) },
            enabled = !selectionState.value,
            onClick = {
                navigate(Screen.FavoriteScreen.route)
                expandedDropDown.value = false
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.trash)) },
            enabled = !selectionState.value,
            onClick = {
                navigate(Screen.TrashedScreen.route)
                expandedDropDown.value = false
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.settings_title)) },
            enabled = !selectionState.value,
            onClick = {
                navigate(Screen.SettingsScreen.route)
                expandedDropDown.value = false
            }
        )
    }
}

