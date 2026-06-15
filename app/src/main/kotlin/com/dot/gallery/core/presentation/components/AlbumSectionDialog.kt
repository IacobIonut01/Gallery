/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.AlbumSectionWithAlbums
import com.dot.gallery.feature_node.presentation.common.components.OptionButton
import com.dot.gallery.feature_node.presentation.common.components.OptionPosition
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import kotlinx.coroutines.launch

enum class SectionDialogMode {
    Create, Rename
}

/**
 * Bottom sheet for creating or renaming an album section.
 */
@Composable
fun AlbumSectionNameSheet(
    sheetState: AppBottomSheetState,
    mode: SectionDialogMode = SectionDialogMode.Create,
    initialName: String = "",
    onCreateSection: (String) -> Unit = {},
    onRenameSection: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var sectionName by remember(initialName) { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }

    val titleRes = when (mode) {
        SectionDialogMode.Rename -> R.string.rename_section
        SectionDialogMode.Create -> R.string.create_section
    }

    ModalSheet(
        sheetState = sheetState,
        title = stringResource(titleRes),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = {
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text(stringResource(R.string.section_name)) },
                placeholder = { Text(stringResource(R.string.section_name_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .imePadding(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (sectionName.isNotBlank()) {
                            scope.launch {
                                sheetState.hide()
                                when (mode) {
                                    SectionDialogMode.Rename -> onRenameSection(sectionName.trim())
                                    SectionDialogMode.Create -> onCreateSection(sectionName.trim())
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
                enabled = sectionName.isNotBlank(),
                text = stringResource(titleRes),
                onClick = {
                    if (sectionName.isNotBlank()) {
                        scope.launch {
                            sheetState.hide()
                            when (mode) {
                                SectionDialogMode.Rename -> onRenameSection(sectionName.trim())
                                SectionDialogMode.Create -> onCreateSection(sectionName.trim())
                            }
                        }
                    }
                }
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    )
}

/**
 * Bottom sheet for deleting an album section.
 */
@Composable
fun DeleteSectionSheet(
    sheetState: AppBottomSheetState,
    onConfirmDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalSheet(
        sheetState = sheetState,
        title = stringResource(R.string.delete_section),
        subtitle = stringResource(R.string.delete_section_confirm),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = {
            OptionButton(
                icon = Icons.Outlined.Delete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                textContainer = {
                    Text(stringResource(R.string.delete_section))
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

/**
 * Bottom sheet for moving an album to a section.
 * Shows all sections with the current one highlighted.
 * "Automatic" is the first option to let the classifier decide.
 */
@Composable
fun MoveToSectionSheet(
    sheetState: AppBottomSheetState,
    sections: List<AlbumSectionWithAlbums>,
    currentSectionId: Long?,
    onMoveToSection: (Long) -> Unit,
    onResetOverride: () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalSheet(
        sheetState = sheetState,
        title = stringResource(R.string.move_to_section),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = {
            // "Automatic" option first
            val isAuto = currentSectionId == null
            OptionButton(
                icon = Icons.Outlined.AutoAwesome,
                textContainer = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.section_auto),
                            fontWeight = if (isAuto) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isAuto) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                position = if (sections.isEmpty()) OptionPosition.ALONE else OptionPosition.TOP,
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onResetOverride()
                    }
                }
            )

            // Section options
            sections.forEachIndexed { index, swa ->
                val isCurrent = swa.section.id == currentSectionId
                val position = when {
                    index == sections.lastIndex -> OptionPosition.BOTTOM
                    else -> OptionPosition.MIDDLE
                }
                OptionButton(
                    icon = Icons.Outlined.Folder,
                    textContainer = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = swa.section.label,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    position = position,
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onMoveToSection(swa.section.id)
                        }
                    }
                )
            }
        }
    )
}
