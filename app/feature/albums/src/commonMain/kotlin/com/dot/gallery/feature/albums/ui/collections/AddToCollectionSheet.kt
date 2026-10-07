/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature.albums.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.domain.model.CollectionWithCount
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.albums.generated.resources.Res
import com.dot.gallery.feature.albums.generated.resources.add_to_collection
import com.dot.gallery.feature.albums.generated.resources.collection_name
import com.dot.gallery.feature.albums.generated.resources.collection_name_hint
import com.dot.gallery.feature.albums.generated.resources.create_and_add
import com.dot.gallery.feature.albums.generated.resources.n_items_in_collection
import com.dot.gallery.feature.albums.generated.resources.new_collection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCollectionSheet(
    visible: Boolean,
    collections: List<CollectionWithCount>,
    onDismiss: () -> Unit,
    onCollectionSelected: (Long) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    collectionThumbnailModel: CollectionThumbnailModel = { null },
) {
    if (!visible) return

    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    var showCreateField by rememberSaveable { mutableStateOf(false) }
    var newCollectionName by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = {
            showCreateField = false
            newCollectionName = ""
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(Res.string.add_to_collection),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (showCreateField) {
                val focusRequester = remember { FocusRequester() }
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        label = { Text(stringResource(Res.string.collection_name)) },
                        placeholder = { Text(stringResource(Res.string.collection_name_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newCollectionName.isNotBlank()) {
                                    onCreateAndAdd(newCollectionName.trim())
                                    showCreateField = false
                                    newCollectionName = ""
                                    onDismiss()
                                }
                            }
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        enabled = newCollectionName.isNotBlank(),
                        onClick = {
                            onCreateAndAdd(newCollectionName.trim())
                            showCreateField = false
                            newCollectionName = ""
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.create_and_add))
                    }
                }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Spacer(Modifier.height(12.dp))
            }

            // Create new collection button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showCreateField = !showCreateField }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.new_collection),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            // Existing collections
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = collections,
                    key = { it.collection.id }
                ) { cwc ->
                    val thumbnailModel = remember(cwc.thumbnailMediaId) {
                        cwc.thumbnailMediaId?.let(collectionThumbnailModel)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onCollectionSelected(cwc.collection.id)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (thumbnailModel != null) {
                            val renderer = LocalMediaImageRenderer.current
                            renderer.RenderImage(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                model = thumbnailModel,
                                contentDescription = cwc.collection.label,
                                contentScale = ContentScale.Crop,
                                signature = cwc
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Collections,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cwc.collection.label,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(Res.string.n_items_in_collection, cwc.mediaCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
