/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.ui.components.ChooserPreferenceDetailScreen
import com.dot.gallery.core.ui.components.SettingsItem
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.settings.generated.resources.Res
import com.dot.gallery.feature.settings.generated.resources.smart_features_cloud_indexing_description
import com.dot.gallery.feature.settings.generated.resources.smart_features_cloud_section
import com.dot.gallery.feature.settings.generated.resources.smart_features_index_on_device
import com.dot.gallery.feature.settings.generated.resources.smart_features_index_on_device_delegated_summary
import com.dot.gallery.feature.settings.generated.resources.smart_features_index_on_device_section
import com.dot.gallery.feature.settings.generated.resources.smart_features_index_on_device_summary
import com.dot.gallery.feature.settings.generated.resources.smart_features_provider_smart_search
import com.dot.gallery.feature.settings.generated.resources.smart_features_provider_smart_search_summary

@Composable
fun SettingsCloudIndexingContent(
    state: SettingsCloudIndexingState,
    actions: SettingsCloudIndexingActions = SettingsCloudIndexingActions(),
) {
    ChooserPreferenceDetailScreen<Unit>(
        title = stringResource(Res.string.smart_features_cloud_section),
        description = stringResource(Res.string.smart_features_cloud_indexing_description),
        customContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsItem(
                    item = SettingsEntity.SwitchPreference(
                        title = stringResource(Res.string.smart_features_provider_smart_search),
                        summary = stringResource(Res.string.smart_features_provider_smart_search_summary),
                        isChecked = state.providerSmartSearch,
                        onCheck = actions.onProviderSmartSearchChange,
                        screenPosition = Position.Alone
                    )
                )
                if (state.providers.isNotEmpty()) {
                    SettingsItem(
                        item = SettingsEntity.Header(
                            title = stringResource(Res.string.smart_features_index_on_device_section)
                        )
                    )
                    state.providers.forEachIndexed { index, provider ->
                        SettingsItem(
                            item = SettingsEntity.SwitchPreference(
                                title = stringResource(
                                    Res.string.smart_features_index_on_device,
                                    provider.type.displayName
                                ),
                                summary = if (provider.delegatesSmartSearch) {
                                    stringResource(
                                        Res.string.smart_features_index_on_device_delegated_summary,
                                        provider.type.displayName
                                    )
                                } else {
                                    stringResource(Res.string.smart_features_index_on_device_summary)
                                },
                                isChecked = provider.indexOnDevice,
                                onCheck = {
                                    actions.onIndexOnDeviceChange(provider.type, it)
                                },
                                screenPosition = when {
                                    state.providers.size == 1 -> Position.Alone
                                    index == 0 -> Position.Top
                                    index == state.providers.lastIndex -> Position.Bottom
                                    else -> Position.Middle
                                }
                            )
                        )
                    }
                }
            }
        }
    )
}
