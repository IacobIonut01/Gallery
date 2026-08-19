/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen

@Composable
fun CloudAdvancedSettingsScreen(configId: Long) {
    val settingsVm = hiltViewModel<CloudSettingsViewModel>()
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()
    val cacheClearing by settingsVm.cacheClearing.collectAsStateWithLifecycle()

    LaunchedEffect(configId) { settingsVm.loadConfig(configId) }
    if (config == null) {
        AccountSettingsStateScreen(stringResource(R.string.cloud_advanced), loadState)
        return
    }
    val troubleshooting = config?.verboseLogging ?: false
    val readOnlyMode = config?.readOnlyMode ?: false
    val troubleshootingHeader = stringResource(R.string.cloud_adv_troubleshooting)
    val verboseTitle = stringResource(R.string.cloud_adv_verbose)
    val verboseSummary = stringResource(R.string.cloud_adv_verbose_summary)
    val displayHeader = stringResource(R.string.cloud_adv_display)
    val readOnlyTitle = stringResource(R.string.cloud_adv_readonly)
    val readOnlySummary = stringResource(R.string.cloud_adv_readonly_summary)
    val cacheHeader = stringResource(R.string.cloud_global) + " · " +
        stringResource(R.string.cloud_adv_cache)
    val clearingCacheTitle = stringResource(R.string.cloud_adv_clearing_cache)
    val clearCacheTitle = stringResource(R.string.cloud_adv_clear_cache)
    val resourceStrings = listOf(
        troubleshootingHeader,
        verboseTitle,
        verboseSummary,
        displayHeader,
        readOnlyTitle,
        readOnlySummary,
        cacheHeader,
        clearingCacheTitle,
        clearCacheTitle,
    )

    val settingsList = remember(
        troubleshooting,
        readOnlyMode,
        cacheClearing,
        resourceStrings,
    ) {
        buildList {
            // Troubleshooting section
            add(SettingsEntity.Header(title = troubleshootingHeader))
            add(
                SettingsEntity.SwitchPreference(
                    title = verboseTitle,
                    summary = verboseSummary,
                    isChecked = troubleshooting,
                    onCheck = { settingsVm.updateConfig { copy(verboseLogging = it) } },
                    screenPosition = Position.Alone
                )
            )

            // Display section
            add(SettingsEntity.Header(title = displayHeader))
            add(
                SettingsEntity.SwitchPreference(
                    title = readOnlyTitle,
                    summary = readOnlySummary,
                    isChecked = readOnlyMode,
                    onCheck = { settingsVm.updateConfig { copy(readOnlyMode = it) } },
                    screenPosition = Position.Alone
                )
            )

            // Cache section
            add(SettingsEntity.Header(title = cacheHeader))
            add(
                SettingsEntity.Preference(
                    title = if (cacheClearing) clearingCacheTitle else clearCacheTitle,
                    enabled = !cacheClearing,
                    onClick = { settingsVm.clearImageCache() },
                    screenPosition = Position.Alone
                )
            )
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(R.string.cloud_advanced),
        settingsList = settingsList
    )
}
