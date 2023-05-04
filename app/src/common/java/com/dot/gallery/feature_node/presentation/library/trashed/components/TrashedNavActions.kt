package com.dot.gallery.feature_node.presentation.library.trashed.components

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashedNavActions(
    handler: MediaHandleUseCase,
    mediaState: MutableState<MediaState>,
    selectedMedia: SnapshotStateList<Media>,
    selectionState: MutableState<Boolean>,
) {
    val state by mediaState
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isMediaManager = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaStore.canManageMedia(context)
        else false
    }
    val openBottomDeleteSheet = remember { mutableStateOf(false) }
    val openBottomRestoreSheet = remember { mutableStateOf(false) }
    val deleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val restoreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                selectedMedia.clear()
                selectionState.value = false
                if (isMediaManager) {
                    scope.launch {
                        if (openBottomDeleteSheet.value) {
                            deleteSheetState.hide()
                            openBottomDeleteSheet.value = false
                        } else if (openBottomRestoreSheet.value) {
                            restoreSheetState.hide()
                            openBottomRestoreSheet.value = false
                        }
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
                        openBottomRestoreSheet.value = true
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
                            openBottomDeleteSheet.value = true
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
                            openBottomDeleteSheet.value = true
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
        openBottomSheet = openBottomDeleteSheet,
        sheetState = deleteSheetState,
        data = selectedMedia.ifEmpty { state.media },
        onConfirm = {
            handler.deleteMedia(result, it)
        }
    )
    TrashDialog(
        openBottomSheet = openBottomRestoreSheet,
        sheetState = restoreSheetState,
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

