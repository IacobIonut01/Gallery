/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.albums.ui.collections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.ModalSheet
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.util.AppBottomSheetState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.albums.generated.resources.Res
import com.dot.gallery.feature.albums.generated.resources.collection_name
import com.dot.gallery.feature.albums.generated.resources.collection_name_hint
import com.dot.gallery.feature.albums.generated.resources.create_collection
import com.dot.gallery.feature.albums.generated.resources.delete_collection
import com.dot.gallery.feature.albums.generated.resources.delete_collection_confirm
import com.dot.gallery.feature.albums.generated.resources.new_collection
import com.dot.gallery.feature.albums.generated.resources.rename_collection

@Composable
fun CollectionSheet(
    sheetState: AppBottomSheetState,
    mode: String, // "create" or "rename"
    initialName: String = "",
    onCreateCollection: (String) -> Unit = {},
    onRenameCollection: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var collectionName by remember(initialName) { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }

    ModalSheet(
        sheetState = sheetState,
        title = when (mode) {
            "rename" -> stringResource(Res.string.rename_collection)
            else -> stringResource(Res.string.new_collection)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = {
            OutlinedTextField(
                value = collectionName,
                onValueChange = { collectionName = it },
                label = { Text(stringResource(Res.string.collection_name)) },
                placeholder = { Text(stringResource(Res.string.collection_name_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .imePadding(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (collectionName.isNotBlank()) {
                            scope.launch {
                                sheetState.hide()
                                when (mode) {
                                    "rename" -> onRenameCollection(collectionName.trim())
                                    else -> onCreateCollection(collectionName.trim())
                                }
                            }
                        }
                    }
                )
            )
            Spacer(Modifier.height(16.dp))
            SetupButton(
                onClick = {
                    if (collectionName.isNotBlank()) {
                        scope.launch {
                            sheetState.hide()
                            when (mode) {
                                "rename" -> onRenameCollection(collectionName.trim())
                                else -> onCreateCollection(collectionName.trim())
                            }
                        }
                    }
                },
                enabled = collectionName.isNotBlank(),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                text = when (mode) {
                    "rename" -> stringResource(Res.string.rename_collection)
                    else -> stringResource(Res.string.create_collection)
                }
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    )
}

@Composable
fun DeleteCollectionSheet(
    sheetState: AppBottomSheetState,
    onConfirmDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalSheet(
        sheetState = sheetState,
        title = stringResource(Res.string.delete_collection),
        subtitle = stringResource(Res.string.delete_collection_confirm),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = {
            SetupButton(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onConfirmDelete()
                    }
                },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                text = stringResource(Res.string.delete_collection)
            )
        }
    )
}
