package com.dot.gallery.feature_node.presentation.customalbums.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.CustomAlbum

@Composable
fun DeleteCustomAlbumDialog(
    album: CustomAlbum,
    onConfirmation: (CustomAlbum) -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(id = R.string.delete_custom_album_dialog_title)
            )
        },
        title = {
            Text(text = stringResource(id = R.string.delete_custom_album_dialog_title))
        },
        text = {
            Column() {
                Text(text = stringResource(id = R.string.delete_custom_album_dialog_description, album.label))
            }
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(album)
                }
            ) {
                Text(stringResource(id = R.string.delete_custom_album_dialog_action_delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }

    )
}
