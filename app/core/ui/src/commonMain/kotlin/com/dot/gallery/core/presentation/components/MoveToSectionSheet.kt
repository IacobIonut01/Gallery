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
import com.dot.gallery.domain.model.AlbumSectionWithAlbums
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.core.ui.generated.resources.Res
import com.dot.gallery.core.ui.generated.resources.move_to_section
import com.dot.gallery.core.ui.generated.resources.section_auto
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
        title = stringResource(Res.string.move_to_section),
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
                            text = stringResource(Res.string.section_auto),
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
