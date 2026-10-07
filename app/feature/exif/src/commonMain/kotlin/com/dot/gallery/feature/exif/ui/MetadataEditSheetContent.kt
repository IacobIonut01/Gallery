/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.isVideo
import com.dot.gallery.ui.theme.Shapes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.exif.generated.resources.Res
import com.dot.gallery.feature.exif.generated.resources.action_cancel
import com.dot.gallery.feature.exif.generated.resources.action_confirm
import com.dot.gallery.feature.exif.generated.resources.description
import com.dot.gallery.feature.exif.generated.resources.edit_metadata
import com.dot.gallery.feature.exif.generated.resources.label
import com.dot.gallery.feature.exif.generated.resources.video_description_local_only

/**
 * Label + description editor sheet. Applying the update (rename, description
 * write, media scan) is the caller's job — the content reports the intent via
 * [onSave].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditSheetContent(
    state: AppBottomSheetState,
    media: Media,
    metadata: MediaMetadata?,
    onSave: (newLabel: String, imageDescription: String?) -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var imageDescription by rememberSaveable(metadata) {
        mutableStateOf(metadata?.imageDescription)
    }
    var newLabel by rememberSaveable { mutableStateOf(media.label) }

    if (state.isVisible) {
        ModalBottomSheet(
            sheetState = state.sheetState,
            onDismissRequest = {
                scope.launch { state.hide() }
            },
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.edit_metadata),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                )

                TextField(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    value = newLabel,
                    onValueChange = { newValue ->
                        newLabel = newValue
                    },
                    label = {
                        Text(text = stringResource(Res.string.label))
                    },
                    singleLine = true,
                    shape = Shapes.large,
                    colors = TextFieldDefaults.colors(
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )

                TextField(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(112.dp),
                    value = imageDescription ?: "",
                    onValueChange = { newValue ->
                        imageDescription = newValue
                    },
                    label = {
                        Text(text = stringResource(Res.string.description))
                    },
                    shape = Shapes.large,
                    colors = TextFieldDefaults.colors(
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )

                // Show info message for videos that description is stored locally only
                AnimatedVisibility(visible = media.isVideo) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.video_description_local_only),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement
                        .spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
                    val tertiaryOnContainer = MaterialTheme.colorScheme.onTertiaryContainer
                    SetupButton(
                        onClick = onCancel,
                        containerColor = tertiaryContainer,
                        contentColor = tertiaryOnContainer,
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        modifier = Modifier.weight(1f),
                        text = stringResource(Res.string.action_cancel)
                    )
                    SetupButton(
                        onClick = { onSave(newLabel, imageDescription) },
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        modifier = Modifier.weight(1f),
                        text = stringResource(Res.string.action_confirm)
                    )
                }
            }
            Spacer(modifier = Modifier)
        }
    }
}
