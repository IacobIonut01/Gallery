/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.app.presentation.util.goBack
import com.dot.gallery.injection.injectViewModel


@Composable
fun CloudAddServerScreen(
    providerType: ProviderType,
    configId: Long? = null,
    onSaved: () -> Unit = {},
    viewModel: CloudAccountsViewModel = injectViewModel { cloudAccountsViewModel }
) {
    val eventHandler = LocalEventHandler.current
    val context = LocalContext.current
    val state by viewModel.addServerState.collectAsStateWithLifecycle()
    val localAlbums by viewModel.localAlbums.collectAsStateWithLifecycle()
    val isEditMode = configId != null && configId > 0

    LaunchedEffect(providerType, configId) {
        if (configId != null && configId > 0) {
            viewModel.initEditServer(configId)
        } else {
            viewModel.initAddServer(providerType)
        }
    }

    val syncCompleted by viewModel.syncCompleted.collectAsStateWithLifecycle()
    LaunchedEffect(syncCompleted) {
        if (syncCompleted) onSaved()
    }

    LaunchedEffect(state.browserLaunchEvent?.id) {
        val event = state.browserLaunchEvent ?: return@LaunchedEffect
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, event.url.toUri()))
            viewModel.consumeBrowserLaunchEvent(event.id)
        } catch (_: ActivityNotFoundException) {
            viewModel.browserLaunchFailed()
        }
    }

    CloudAddServerContent(
        state = state,
        localAlbums = localAlbums,
        isEditMode = isEditMode,
        supportsInteractiveAuth = viewModel.supportsInteractiveAuth(state.providerType),
        actions = CloudAddServerActions(
            updateServerUrl = viewModel::updateServerUrl,
            updateDisplayName = viewModel::updateDisplayName,
            updateApiKey = viewModel::updateApiKey,
            applyBrowserSessionToken = viewModel::applyBrowserSessionToken,
            updateUsername = viewModel::updateUsername,
            updatePassword = viewModel::updatePassword,
            updateAutoUrlSwitch = viewModel::updateAutoUrlSwitch,
            updateLocalWifiSsid = viewModel::updateLocalWifiSsid,
            updateLocalServerUrl = viewModel::updateLocalServerUrl,
            testConnection = viewModel::testConnection,
            startInteractiveAuth = viewModel::startInteractiveAuth,
            cancelInteractiveAuth = viewModel::cancelInteractiveAuth,
            toggleLocalAlbum = viewModel::toggleLocalAlbum,
            saveServer = viewModel::saveServer,
        ),
        onNavigateUp = { eventHandler.navigateUpAction() },
    )
}
