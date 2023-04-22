package com.dot.gallery.core.presentation.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.dot.gallery.feature_node.domain.model.Media

@Composable
fun RowScope.NavigationActions(
    selectedMedia: SnapshotStateList<Media>,
    selectionState: MutableState<Boolean>,
    actions: @Composable (RowScope.(
        expandedDropDown: MutableState<Boolean>,
        selectedMedia: SnapshotStateList<Media>,
        selectionState: MutableState<Boolean>,
        result: ActivityResultLauncher<IntentSenderRequest>
    ) -> Unit),
    onActivityResult: (
        selectedMedia: SnapshotStateList<Media>,
        selectionState: MutableState<Boolean>,
        result: ActivityResult
    ) -> Unit
) {
    val expandedDropDown = rememberSaveable { mutableStateOf(false) }
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            onActivityResult(
                selectedMedia,
                selectionState,
                it
            )
        }
    )
    actions(expandedDropDown, selectedMedia, selectionState, result)
}