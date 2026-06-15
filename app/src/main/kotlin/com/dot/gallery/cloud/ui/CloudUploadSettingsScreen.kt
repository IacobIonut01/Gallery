/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.presentation.ignored.setup.components.SelectableAlbumItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudUploadSettingsScreen(
    viewModel: CloudUploadSettingsViewModel = hiltViewModel(),
    navigateUp: () -> Unit = {}
) {
    val localAlbums by viewModel.localAlbums.collectAsStateWithLifecycle()
    val uploadPrefs by viewModel.uploadPreferences.collectAsStateWithLifecycle()
    val deleteLocalPrefs by viewModel.deleteLocalPreferences.collectAsStateWithLifecycle()

    val uploadRunning by viewModel.uploadWorkRunning.collectAsStateWithLifecycle()

    var showDedupDialog by remember { mutableStateOf(false) }
    val dedupState by viewModel.dedupState.collectAsStateWithLifecycle()

    val enabledAlbums by remember(uploadPrefs) {
        derivedStateOf { uploadPrefs.filter { it.value }.keys }
    }

    val albumGridSize by rememberAlbumGridSize()
    val gridCells = remember(albumGridSize) {
        Constants.albumCellsList[albumGridSize]
    }
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.cloud_upload_albums),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (enabledAlbums.isNotEmpty()) {
                            Text(
                                text = "${enabledAlbums.size} albums selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            columns = gridCells,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Album grid
            if (localAlbums.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "No local albums found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                items(
                    items = localAlbums,
                    key = { it.toString() }
                ) { album ->
                    val isSelected = uploadPrefs[album.id] ?: false
                    SelectableAlbumItem(
                        album = album,
                        isSelected = isSelected,
                        isDisabled = false,
                        showCheckmark = true,
                        onClick = {
                            viewModel.setAlbumUploadEnabled(album.id, album.label, !isSelected)
                        }
                    )
                }
            }

            // Settings + Sync section for enabled albums
            if (enabledAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        // Sync now button
                        SetupButton(
                            text = if (uploadRunning) stringResource(R.string.cloud_upload_syncing)
                                   else stringResource(R.string.cloud_upload_sync_now),
                            enabled = !uploadRunning,
                            applyHorizontalPadding = false,
                            applyBottomPadding = false,
                            applyInsets = false,
                            onClick = { viewModel.triggerUploadNow() }
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.cloud_upload_settings_header),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.cloud_delete_local_after_upload),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.cloud_delete_local_after_upload_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = deleteLocalPrefs.any { it.value },
                                onCheckedChange = { enabled ->
                                    enabledAlbums.forEach { albumId ->
                                        val album = localAlbums.find { it.id == albumId }
                                        if (album != null) {
                                            viewModel.setDeleteLocalEnabled(album.id, album.label, enabled)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Dedup section
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cloud_dedup_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                    SetupButton(
                        text = stringResource(R.string.cloud_find_duplicates),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        onClick = { showDedupDialog = true; viewModel.findDuplicates() }
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (showDedupDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!dedupState.isScanning && !dedupState.isDeleting) {
                    showDedupDialog = false
                    viewModel.clearDedupState()
                }
            },
            title = { Text(stringResource(R.string.cloud_find_duplicates)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(dedupState.message)
                    if (dedupState.isScanning && dedupState.totalCount > 0) {
                        LinearProgressIndicator(
                            progress = { dedupState.scannedCount.toFloat() / dedupState.totalCount },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (dedupState.isScanning || dedupState.isDeleting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (dedupState.duplicates.isNotEmpty() && !dedupState.isDeleting) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${dedupState.duplicates.size} files can be removed locally",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                if (dedupState.duplicates.isNotEmpty() && !dedupState.isScanning && !dedupState.isDeleting) {
                    Button(onClick = { viewModel.deleteLocalDuplicates() }) {
                        Text(stringResource(R.string.cloud_dedup_delete_local))
                    }
                }
            },
            dismissButton = {
                if (!dedupState.isScanning && !dedupState.isDeleting) {
                    TextButton(onClick = {
                        showDedupDialog = false
                        viewModel.clearDedupState()
                    }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        )
    }
}
