/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.trashed.components

import com.dot.gallery.cloud.ui.CloudSelectionViewModel

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.data.repository.MediaMutationResult
import kotlinx.coroutines.launch
import com.dot.gallery.domain.trash.TrashDialogAction
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.core.presentation.components.TrashDialog
import com.dot.gallery.injection.injectViewModel


@Composable
fun <T : Media> AutoDeleteFooter(
    mediaState: State<MediaState<T>>,
) {
    val cloudSelectionViewModel = injectViewModel<CloudSelectionViewModel> {cloudSelectionViewModel}
    val handler = LocalMediaHandler.current
    val selector = LocalMediaSelector.current
    val scope = rememberCoroutineScope()
    val deleteSheetState = rememberAppBottomSheetState()
    val restoreSheetState = rememberAppBottomSheetState()
    val media = mediaState.value.media
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                selector.clearSelection()
                scope.launch {
                    deleteSheetState.hide()
                    restoreSheetState.hide()
                }
            }
        }
    )

    TrashFooterContent(
        hasMedia = media.isNotEmpty(),
        onRestoreAll = { scope.launch { restoreSheetState.show() } },
        onDeleteAll = { scope.launch { deleteSheetState.show() } },
    )

    TrashDialog(
        appBottomSheetState = deleteSheetState,
        data = media,
        action = TrashDialogAction.DELETE,
        supportsCloudTrash = { cloudSelectionViewModel.supportsTrash(it) },
    ) {
        if (handler.deleteMedia(result, it) == MediaMutationResult.COMPLETED) {
            selector.clearSelection()
        }
    }
    TrashDialog(
        appBottomSheetState = restoreSheetState,
        data = media,
        action = TrashDialogAction.RESTORE,
        supportsCloudTrash = { cloudSelectionViewModel.supportsTrash(it) },
    ) {
        if (handler.trashMedia(result, it, false) == MediaMutationResult.COMPLETED) {
            selector.clearSelection()
        }
    }
}