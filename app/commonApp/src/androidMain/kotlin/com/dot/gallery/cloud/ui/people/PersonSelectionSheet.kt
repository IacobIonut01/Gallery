/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.core.presentation.components.SelectionBarColumn
import com.dot.gallery.core.presentation.components.SelectionSheet
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.core.presentation.util.selectedMedia
import com.dot.gallery.core.presentation.components.ConfirmationSheet
import kotlinx.coroutines.launch
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_person_remove_media
import com.dot.gallery.generated.resources.cloud_person_remove_media_confirm
import com.dot.gallery.generated.resources.cloud_person_remove_media_summary
import com.dot.gallery.generated.resources.cloud_person_remove_media_title


/**
 * Selection sheet shown on a local person's detail grid — the standard selection actions
 * plus a "Remove from person" action that un-assigns the selected media's faces from this
 * person (without deleting the media).
 */
@Composable
fun BoxScope.PersonSelectionSheet(
    allMedia: MediaState<UriMedia>,
    personName: String,
    onRemoveFromPerson: (List<Long>) -> Unit,
) {
    val selector = LocalMediaSelector.current
    val selectedMedia = selector.selectedMedia.collectAsStateWithLifecycle()
    val selectedMediaList = allMedia.media.selectedMedia(selectedSet = selectedMedia)
    val scope = rememberCoroutineScope()
    val removeConfirmState = rememberAppBottomSheetState()

    SelectionSheet(
        modifier = Modifier.align(Alignment.BottomEnd),
        allMedia = allMedia,
        selectedMedia = selectedMediaList,
        extraBottomActions = {
            SelectionBarColumn(
                imageVector = Icons.Outlined.PersonRemove,
                title = stringResource(Res.string.cloud_person_remove_media)
            ) {
                scope.launch { removeConfirmState.show() }
            }
        }
    )

    ConfirmationSheet(
        state = removeConfirmState,
        title = pluralStringResource(
            Res.plurals.cloud_person_remove_media_title,
            selectedMediaList.size,
            selectedMediaList.size,
            personName
        ),
        summary = stringResource(Res.string.cloud_person_remove_media_summary),
        confirmText = stringResource(Res.string.cloud_person_remove_media_confirm),
        onConfirm = {
            onRemoveFromPerson(selectedMediaList.map { it.id })
        }
    )
}
