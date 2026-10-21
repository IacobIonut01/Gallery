/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.offline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.injection.injectViewModel


@Composable
fun OfflineModeScreen(
    viewModel: OfflineModeViewModel = injectViewModel { offlineModeViewModel }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accountSheet by viewModel.accountSheet.collectAsStateWithLifecycle()
    OfflineModeContent(
        state = state,
        accountSheet = accountSheet,
        actions = OfflineModeContentActions(
            setForceOffline = viewModel::setForceOffline,
            setCacheOnView = viewModel::setCacheOnView,
            setCacheWifiOnly = viewModel::setCacheWifiOnly,
            setDownloadFavoritesFullRes = viewModel::setDownloadFavoritesFullRes,
            setBudgetMb = viewModel::setBudgetMb,
            pinAccount = viewModel::pinAccount,
            unpinAccount = viewModel::unpinAccount,
            downloadNow = viewModel::downloadNow,
            openAccountCache = viewModel::openAccountCache,
            closeAccountCache = viewModel::closeAccountCache,
            clearAccountCache = viewModel::clearAccountCache,
            clearAlbumCache = viewModel::clearAlbumCache,
            clearAllCache = viewModel::clearAllCache,
            clearAutoCache = viewModel::clearAutoCache,
        ),
    )
}
