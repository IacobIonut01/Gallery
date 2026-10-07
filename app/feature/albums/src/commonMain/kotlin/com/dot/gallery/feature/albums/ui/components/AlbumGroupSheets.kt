/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.albums.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.ModalSheet
import com.dot.gallery.core.presentation.components.OptionButton
import com.dot.gallery.core.presentation.components.OptionItem
import com.dot.gallery.core.presentation.components.OptionLayout
import com.dot.gallery.core.presentation.components.OptionPosition
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.domain.model.AlbumGroupWithAlbums
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.albums.generated.resources.Res
import com.dot.gallery.feature.albums.generated.resources.add_to_group
import com.dot.gallery.feature.albums.generated.resources.create_album_group
import com.dot.gallery.feature.albums.generated.resources.create_new_group
import com.dot.gallery.feature.albums.generated.resources.delete_group
import com.dot.gallery.feature.albums.generated.resources.delete_group_confirm
import com.dot.gallery.feature.albums.generated.resources.group_name
import com.dot.gallery.feature.albums.generated.resources.group_name_hint
import com.dot.gallery.feature.albums.generated.resources.rename_group

@Composable
fun AlbumGroupSheet(
    sheetState: AppBottomSheetState,
    mode: String, // "create", "rename", "addToGroup"
    initialName: String = "",
    existingGroups: List<AlbumGroupWithAlbums> = emptyList(),
    onCreateGroup: (String) -> Unit = {},
    onRenameGroup: (String) -> Unit = {},
    onAddToExistingGroup: (Long) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var groupName by remember(initialName) { mutableStateOf(initialName) }
    var showCreateNew by remember(mode) { mutableStateOf(mode == "create" || mode == "rename") }
    val focusRequester = remember { FocusRequester() }

    ModalSheet(
        sheetState = sheetState,
        title = when (mode) {
            "rename" -> stringResource(Res.string.rename_group)
            "addToGroup" -> stringResource(Res.string.add_to_group)
            else -> stringResource(Res.string.create_album_group)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = {
            if (mode == "addToGroup" && existingGroups.isNotEmpty() && !showCreateNew) {
                val groupOptions = remember(existingGroups) {
                    existingGroups.map { group ->
                        OptionItem(
                            icon = Icons.Outlined.Collections,
                            text = group.group.label,
                            summary = "${group.albums.size} albums",
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                    onAddToExistingGroup(group.group.id)
                                }
                            }
                        )
                    }.toMutableStateList()
                }
                OptionLayout(
                    modifier = Modifier.fillMaxWidth(),
                    optionList = groupOptions
                )
                Spacer(Modifier.height(8.dp))
                SetupButton(
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    text = stringResource(Res.string.create_new_group),
                    onClick = { showCreateNew = true }
                )
            } else {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(stringResource(Res.string.group_name)) },
                    placeholder = { Text(stringResource(Res.string.group_name_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .imePadding(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (groupName.isNotBlank()) {
                                scope.launch {
                                    sheetState.hide()
                                    when (mode) {
                                        "rename" -> onRenameGroup(groupName.trim())
                                        else -> onCreateGroup(groupName.trim())
                                    }
                                }
                            }
                        }
                    )
                )
                Spacer(Modifier.height(16.dp))
                SetupButton(
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    enabled = groupName.isNotBlank(),
                    text = when (mode) {
                        "rename" -> stringResource(Res.string.rename_group)
                        else -> stringResource(Res.string.create_new_group)
                    },
                    onClick = {
                        if (groupName.isNotBlank()) {
                            scope.launch {
                                sheetState.hide()
                                when (mode) {
                                    "rename" -> onRenameGroup(groupName.trim())
                                    else -> onCreateGroup(groupName.trim())
                                }
                            }
                        }
                    }
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    )
}

@Composable
fun DeleteGroupSheet(
    sheetState: AppBottomSheetState,
    onConfirmDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalSheet(
        sheetState = sheetState,
        title = stringResource(Res.string.delete_group),
        subtitle = stringResource(Res.string.delete_group_confirm),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = {
            OptionButton(
                icon = Icons.Outlined.Delete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                textContainer = {
                    Text(stringResource(Res.string.delete_group))
                },
                position = OptionPosition.ALONE,
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onConfirmDelete()
                    }
                }
            )
        }
    )
}
