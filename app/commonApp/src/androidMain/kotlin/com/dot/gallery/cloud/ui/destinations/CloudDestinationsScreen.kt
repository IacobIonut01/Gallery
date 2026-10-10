/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.destinations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.injection.injectViewModel


@Composable
fun CloudDestinationsScreen(
    onAddAccount: () -> Unit = {},
    viewModel: CloudDestinationsViewModel = injectViewModel { cloudDestinationsViewModel(it) }
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val albums by viewModel.localAlbums.collectAsStateWithLifecycle()
    val enabledByAlbum by viewModel.enabledByAlbum.collectAsStateWithLifecycle()
    val deleteLocalByAlbum by viewModel.deleteLocalByAlbum.collectAsStateWithLifecycle()
    CloudDestinationsContent(
        accounts = accounts,
        albums = albums,
        enabledByAlbum = enabledByAlbum,
        deleteLocalByAlbum = deleteLocalByAlbum,
        filterConfigId = viewModel.filterConfigId,
        onAddAccount = onAddAccount,
        onSetDeleteLocal = viewModel::setDeleteLocal,
        onSetDestination = viewModel::setDestination,
        onSetAlbumAllClouds = viewModel::setAlbumAllClouds,
    )
}
