package com.dot.gallery.feature_node.presentation.timeline.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.MediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.library.trashed.components.TrashDialog
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.rememberIsMediaManager
import com.dot.gallery.feature_node.presentation.util.shareMedia
import kotlinx.coroutines.launch

@Composable
fun RowScope.TimelineNavActions(
    albumId: Long,
    handler: MediaHandleUseCase,
    expandedDropDown: MutableState<Boolean>,
    mediaState: MutableState<MediaState>,
    selectedMedia: SnapshotStateList<Media>,
    selectionState: MutableState<Boolean>,
    navigate: (route: String) -> Unit,
    navigateUp: () -> Unit
) {
    val shareMedia = stringResource(id = R.string.share_media)
    val trashMedia = stringResource(R.string.trash)
    val context = LocalContext.current
    val state by mediaState
    val scope = rememberCoroutineScope()
    val isMediaManager = rememberIsMediaManager()
    val trashState = rememberAppBottomSheetState()
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                selectedMedia.clear()
                selectionState.value = false
                if (isMediaManager) {
                    scope.launch {
                        trashState.hide()
                    }
                }
            }
        }
    )
    AnimatedVisibility(
        visible = selectionState.value,
        enter = Constants.Animation.enterAnimation,
        exit = Constants.Animation.exitAnimation
    ) {
        Row {
            IconButton(
                onClick = {
                    scope.launch {
                        context.shareMedia(selectedMedia)
                        selectionState.value = false
                        selectedMedia.clear()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = shareMedia
                )
            }
            IconButton(
                onClick = {
                    scope.launch {
                        handler.toggleFavorite(
                            result,
                            selectedMedia
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = trashMedia
                )
            }
            IconButton(
                onClick = {
                    scope.launch {
                        if (isMediaManager) {
                            trashState.show()
                        } else {
                            handler.trashMedia(result, selectedMedia)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = trashMedia
                )
            }
        }
    }
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
            onClick = {
                navigate(Screen.FavoriteScreen.route)
                expandedDropDown.value = false
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.trash)) },
            onClick = {
                navigate(Screen.TrashedScreen.route)
                expandedDropDown.value = false
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.settings_title)) },
            onClick = {
                navigate(Screen.SettingsScreen.route)
                expandedDropDown.value = false
            }
        )
    }
    TrashDialog(
        appBottomSheetState = trashState,
        data = selectedMedia,
        defaultText = {
            stringResource(R.string.trash_dialog_title, it)
        },
        confirmedText = {
            stringResource(R.string.trash_dialog_title_confirmation, it)
        },
        image = Icons.Outlined.DeleteOutline,
        onConfirm = {
            handler.trashMedia(result, it, true)
        }
    )
}

