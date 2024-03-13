package com.dot.gallery.feature_node.presentation.customalbums.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
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

@Composable
fun NewCustomAlbumDialog(
    onConfirmation: (String) -> Unit,
    onDismiss: () -> Unit
) {

    var textfield by remember { mutableStateOf("") }

    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Outlined.AddPhotoAlternate,
                contentDescription = stringResource(id = R.string.custom_album_dialog_newalbum)
            )
        },
        title = {
            Text(text = stringResource(id = R.string.custom_album_dialog_newalbum))
        },
        text = {
            Column() {
                Text(text = stringResource(id = R.string.custom_album_dialog_input_description))
                OutlinedTextField(
                    value = textfield,
                    onValueChange = { textfield = it },
                    label = { Text(text = stringResource(id = R.string.custom_album_dialog_input_title)) },
                    singleLine = true
                )

            }
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(textfield)
                }
            ) {
                Text(stringResource(id = R.string.action_add))
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
