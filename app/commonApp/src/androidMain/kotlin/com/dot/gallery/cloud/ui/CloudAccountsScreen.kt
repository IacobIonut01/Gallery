/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.ui.descriptor.ProviderUiDescriptors
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.navigate
import com.dot.gallery.core.presentation.util.Screen
import com.dot.gallery.core.presentation.util.connectivityState
import com.dot.gallery.core.presentation.util.isLanRouteAvailable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.dot.gallery.cloud.ui.settings.CloudProviderSettingsScreen
import com.dot.gallery.cloud.core.availableRemoteTypes
import com.dot.gallery.injection.injectViewModel


@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun CloudAccountsScreen(
    viewModel: CloudAccountsViewModel = injectViewModel { cloudAccountsViewModel }
) {
    val configs by viewModel.accountState.collectAsStateWithLifecycle()
    val connectionStates by viewModel.connectionStates.collectAsStateWithLifecycle()
    val storageInfoMap by viewModel.storageInfo.collectAsStateWithLifecycle()
    val syncProgressMap by viewModel.syncProgress.collectAsStateWithLifecycle()
    val assetCounts by viewModel.assetCounts.collectAsStateWithLifecycle()
    val serverVersions by viewModel.serverVersions.collectAsStateWithLifecycle()
    val eventHandler = LocalEventHandler.current

    LaunchedEffect(configs) {
        if (configs.isNotEmpty()) {
            viewModel.loadStorageInfo()
            viewModel.loadAssetCounts()
            viewModel.loadServerVersions()
        }
    }

    val remoteProviderTypes = remember { ProviderType.availableRemoteTypes() }

    val networkState = connectivityState()
    val context = LocalContext.current
    val onLocalNetwork = remember(networkState.value) { context.isLanRouteAvailable() }

    // Resolve capabilities only for provider types actually configured — the same
    // registry lookups the rendered cards need, nothing else is instantiated.
    val capabilitiesByType = remember(configs) {
        configs.map { it.providerType }.distinct()
            .associateWith { viewModel.capabilitiesOf(it) }
    }

    CloudAccountsContent(
        state = CloudAccountsContentState(
            configs = configs,
            connectionStates = connectionStates,
            storageInfo = storageInfoMap,
            syncProgress = syncProgressMap,
            assetCounts = assetCounts,
            serverVersions = serverVersions,
            availableProviders = remoteProviderTypes,
            capabilitiesByType = capabilitiesByType,
            onLocalNetwork = onLocalNetwork,
        ),
        actions = CloudAccountsContentActions(
            onAdd = { providerType ->
                eventHandler.navigate(
                    Screen.CloudAddServerScreen.providerType(providerType.name)
                )
            },
            onSettings = { configId ->
                eventHandler.navigate(
                    Screen.CloudProviderSettingsScreen.configId(configId)
                )
            },
            onSync = { configId -> viewModel.triggerSync(configId) },
        ),
    )
}
