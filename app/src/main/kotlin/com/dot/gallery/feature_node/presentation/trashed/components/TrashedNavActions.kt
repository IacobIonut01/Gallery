/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.trashed.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.rememberIsMediaManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun TrashedNavActions(
    handler: MediaHandleUseCase,
    mediaState: StateFlow<MediaState>,
    selectedMedia: SnapshotStateList<Media>,
    selectionState: MutableState<Boolean>,
) {
    val state by mediaState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val isMediaManager = rememberIsMediaManager()
    val deleteSheetState = rememberAppBottomSheetState()
    val restoreSheetState = rememberAppBottomSheetState()
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                selectedMedia.clear()
                selectionState.value = false
                if (isMediaManager) {
                    scope.launch {
                        deleteSheetState.hide()
                        restoreSheetState.hide()
                    }
                }
            }
        }
    )
    if (state.media.isNotEmpty()) {
        TextButton(
            onClick = {
                scope.launch {
                    if (isMediaManager) {
                        restoreSheetState.show()
                    } else {
                        handler.trashMedia(
                            result,
                            selectedMedia.ifEmpty { state.media },
                            false
                        )
                    }
                }
            }
        ) {
            Text(
                text = stringResource(R.string.trash_restore),
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (selectionState.value) {
            TextButton(
                onClick = {
                    scope.launch {
                        if (isMediaManager) {
                            deleteSheetState.show()
                        } else {
                            handler.deleteMedia(result, selectedMedia)
                        }
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.trash_delete),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            TextButton(
                onClick = {
                    scope.launch {
                        if (isMediaManager) {
                            deleteSheetState.show()
                        } else {
                            handler.deleteMedia(result, state.media)
                        }
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.trash_empty),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    TrashDialog(
        appBottomSheetState = deleteSheetState,
        data = selectedMedia.ifEmpty { state.media },
        onConfirm = {
            handler.deleteMedia(result, it)
        }
    )
    TrashDialog(
        appBottomSheetState = restoreSheetState,
        data = selectedMedia.ifEmpty { state.media },
        defaultText = {
            stringResource(R.string.restore_dialog_title, it)
        },
        confirmedText = {
            stringResource(R.string.restore_dialog_title_confirmation, it)
        },
        image = Icons.Outlined.RestoreFromTrash,
        onConfirm = {
            handler.trashMedia(result, it, false)
        }
    )
}

