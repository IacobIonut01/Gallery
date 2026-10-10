/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.injection.injectViewModel


@Composable
fun SyncStatusScreen() {
    val viewModel = injectViewModel<SyncStatusViewModel> {syncStatusViewModel}
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val synced by viewModel.syncedMedia.collectAsStateWithLifecycle()
    val remoteOnly by viewModel.remoteOnlyMedia.collectAsStateWithLifecycle()
    val pendingUpload by viewModel.pendingUploadMedia.collectAsStateWithLifecycle()
    SyncStatusContent(
        state = state,
        synced = synced,
        remoteOnly = remoteOnly,
        pendingUpload = pendingUpload,
        onSync = viewModel::triggerSync,
        onDownloadAll = viewModel::downloadAll,
    )
}
