/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.settings.subsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.feature.settings.ui.CloudIndexingProviderState
import com.dot.gallery.feature.settings.ui.SettingsCloudIndexingActions
import com.dot.gallery.feature.settings.ui.SettingsCloudIndexingContent
import com.dot.gallery.feature.settings.ui.SettingsCloudIndexingState
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.feature.settings.subsettings.SmartFeaturesViewModel

@Composable
fun SettingsCloudIndexingScreen(
    viewModel: SmartFeaturesViewModel = injectViewModel { smartFeaturesViewModel }
) {
    val configuredCloudProviders by viewModel.configuredCloudProviders.collectAsStateWithLifecycle()
    val indexOnDeviceProviders by viewModel.indexOnDeviceProviders.collectAsStateWithLifecycle()
    val providerSmartSearch by viewModel.providerSmartSearch.collectAsStateWithLifecycle()

    SettingsCloudIndexingContent(
        state = SettingsCloudIndexingState(
            providerSmartSearch = providerSmartSearch,
            providers = configuredCloudProviders.map { type ->
                CloudIndexingProviderState(
                    type = type,
                    indexOnDevice = type.name in indexOnDeviceProviders,
                    delegatesSmartSearch =
                        ProviderCapability.SMART_SEARCH in viewModel.capabilitiesOf(type),
                )
            },
        ),
        actions = SettingsCloudIndexingActions(
            onProviderSmartSearchChange = viewModel::setProviderSmartSearch,
            onIndexOnDeviceChange = viewModel::setIndexOnDeviceProvider,
        ),
    )
}
