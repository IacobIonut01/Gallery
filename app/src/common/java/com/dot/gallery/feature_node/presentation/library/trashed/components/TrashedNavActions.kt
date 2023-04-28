package com.dot.gallery.feature_node.presentation.library.trashed.components

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import kotlinx.coroutines.launch

@Composable
fun TrashedNavActions(
    handler: MediaHandleUseCase,
    mediaState: MutableState<MediaState>,
    selectedMedia: SnapshotStateList<Media>,
    selectionState: MutableState<Boolean>,
    result: ActivityResultLauncher<IntentSenderRequest>
) {
    val state by mediaState
    val scope = rememberCoroutineScope()
    if (state.media.isNotEmpty()) {
        TextButton(
            onClick = {
                scope.launch {
                    handler.trashMedia(
                        result,
                        selectedMedia.ifEmpty { state.media },
                        false
                    )
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
                        handler.deleteMedia(result, selectedMedia)
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
                        handler.deleteMedia(result, state.media)
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
}

