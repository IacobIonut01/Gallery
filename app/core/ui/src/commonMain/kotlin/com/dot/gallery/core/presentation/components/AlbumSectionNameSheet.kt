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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.ui.util.AppBottomSheetState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.core.ui.generated.resources.Res
import com.dot.gallery.core.ui.generated.resources.create_section
import com.dot.gallery.core.ui.generated.resources.rename_section
import com.dot.gallery.core.ui.generated.resources.section_name
import com.dot.gallery.core.ui.generated.resources.section_name_hint
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
        SectionDialogMode.Rename -> Res.string.rename_section
        SectionDialogMode.Create -> Res.string.create_section
    }

    ModalSheet(
        sheetState = sheetState,
        title = stringResource(titleRes),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        content = {
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text(stringResource(Res.string.section_name)) },
                placeholder = { Text(stringResource(Res.string.section_name_hint)) },
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
