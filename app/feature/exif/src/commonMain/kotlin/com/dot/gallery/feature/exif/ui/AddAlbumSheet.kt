/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.OptionButton
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.ui.theme.Shapes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.exif.generated.resources.Res
import com.dot.gallery.feature.exif.generated.resources.action_confirm
import com.dot.gallery.feature.exif.generated.resources.album_name
import com.dot.gallery.feature.exif.generated.resources.create_new_album
import com.dot.gallery.feature.exif.generated.resources.location_new_album

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AddAlbumSheet(
    sheetState: AppBottomSheetState,
    hasFullStorageAccess: Boolean,
    onFinish: (albumName: String) -> Unit,
    onCancel: () -> Unit
) {
    var newAlbum: String by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    if (sheetState.isVisible) {
        ModalBottomSheet(
            sheetState = sheetState.sheetState,
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    onCancel()
                }
            },
            dragHandle = { DragHandle() }
        ) {
            BackHandler {
                scope.launch {
                    sheetState.hide()
                    onCancel()
                }
            }
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.create_new_album),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                )

                val location = remember(hasFullStorageAccess, newAlbum) {
                    if (!hasFullStorageAccess) {
                        "Pictures/$newAlbum"
                    } else {
                        newAlbum
                    }
                }

                Text(
                    text = stringResource(Res.string.location_new_album, location),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                )

                TextField(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    value = newAlbum,
                    onValueChange = { newValue ->
                        newAlbum = newValue
                    },
                    label = {
                        Text(text = stringResource(Res.string.album_name))
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

                Spacer(modifier = Modifier.height(16.dp))

                OptionButton(
                    modifier = Modifier
                        .padding(horizontal = 24.dp),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onFinish(newAlbum)
                        }
                    },
                    textContainer = {
                        Text(text = stringResource(Res.string.action_confirm))
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}
