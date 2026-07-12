/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.privatefolder.PrivateFolderMoveViewModel
import com.dot.gallery.feature_node.presentation.trashed.components.TrashDialog
import com.dot.gallery.feature_node.presentation.trashed.components.TrashDialogAction
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import kotlinx.coroutines.launch

/**
 * Delete button for items shown from the private folder. Private-folder media
 * are SAF documents, not MediaStore entries, so deletion must go through
 * [DocumentsContract.deleteDocument] (via [PrivateFolderMoveViewModel]) rather
 * than a MediaStore delete request — the latter crashes on tree document
 * URIs (#1015).
 */
@Composable
fun <T : Media> PrivateFolderDeleteButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onDeleted: () -> Unit = {}
) {
    val viewModel = hiltViewModel<PrivateFolderMoveViewModel>()
    val state = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.DeleteOutline,
        followTheme = followTheme,
        title = stringResource(id = R.string.trash_delete),
        enabled = enabled
    ) {
        scope.launch { state.show() }
    }

    TrashDialog(
        appBottomSheetState = state,
        data = listOf(media),
        action = TrashDialogAction.DELETE
    ) { items ->
        viewModel.deleteFromPrivateFolder(items)
        onDeleted()
    }
}
