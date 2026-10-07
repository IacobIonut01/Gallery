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
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.ui.components.BaseSettingsScreen
import com.dot.gallery.core.ui.components.settings
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_adv_cache
import com.dot.gallery.generated.resources.cloud_adv_clear_cache
import com.dot.gallery.generated.resources.cloud_adv_clearing_cache
import com.dot.gallery.generated.resources.cloud_adv_display
import com.dot.gallery.generated.resources.cloud_adv_readonly
import com.dot.gallery.generated.resources.cloud_adv_readonly_summary
import com.dot.gallery.generated.resources.cloud_adv_sync
import com.dot.gallery.generated.resources.cloud_adv_sync_deletions
import com.dot.gallery.generated.resources.cloud_adv_sync_deletions_summary
import com.dot.gallery.generated.resources.cloud_adv_troubleshooting
import com.dot.gallery.generated.resources.cloud_adv_verbose
import com.dot.gallery.generated.resources.cloud_adv_verbose_summary
import com.dot.gallery.generated.resources.cloud_advanced
import com.dot.gallery.generated.resources.cloud_global


@Composable
fun CloudAdvancedSettingsScreen(configId: Long) {
    val settingsVm = injectViewModel<CloudSettingsViewModel> {cloudSettingsViewModel}
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()
    val cacheClearing by settingsVm.cacheClearing.collectAsStateWithLifecycle()

    LaunchedEffect(configId) { settingsVm.loadConfig(configId) }
    if (config == null) {
        AccountSettingsStateScreen(stringResource(Res.string.cloud_advanced), loadState)
        return
    }
    val troubleshooting = config?.verboseLogging ?: false
    val readOnlyMode = config?.readOnlyMode ?: false
    val syncDeletions = config?.syncRemoteDeletions ?: false
    val troubleshootingHeader = stringResource(Res.string.cloud_adv_troubleshooting)
    val verboseTitle = stringResource(Res.string.cloud_adv_verbose)
    val verboseSummary = stringResource(Res.string.cloud_adv_verbose_summary)
    val displayHeader = stringResource(Res.string.cloud_adv_display)
    val readOnlyTitle = stringResource(Res.string.cloud_adv_readonly)
    val readOnlySummary = stringResource(Res.string.cloud_adv_readonly_summary)
    val syncHeader = stringResource(Res.string.cloud_adv_sync)
    val syncDeletionsTitle = stringResource(Res.string.cloud_adv_sync_deletions)
    val syncDeletionsSummary = stringResource(Res.string.cloud_adv_sync_deletions_summary)
    val cacheHeader = stringResource(Res.string.cloud_global) + " · " +
        stringResource(Res.string.cloud_adv_cache)
    val clearingCacheTitle = stringResource(Res.string.cloud_adv_clearing_cache)
    val clearCacheTitle = stringResource(Res.string.cloud_adv_clear_cache)
    val resourceStrings = listOf(
        troubleshootingHeader,
        verboseTitle,
        verboseSummary,
        displayHeader,
        readOnlyTitle,
        readOnlySummary,
        syncHeader,
        syncDeletionsTitle,
        syncDeletionsSummary,
        cacheHeader,
        clearingCacheTitle,
        clearCacheTitle,
    )

    val settingsList = remember(
        troubleshooting,
        readOnlyMode,
        syncDeletions,
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

            // Sync section
            add(SettingsEntity.Header(title = syncHeader))
            add(
                SettingsEntity.SwitchPreference(
                    title = syncDeletionsTitle,
                    summary = syncDeletionsSummary,
                    isChecked = syncDeletions,
                    onCheck = { settingsVm.updateConfig { copy(syncRemoteDeletions = it) } },
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
        title = stringResource(Res.string.cloud_advanced),
        settingsList = settingsList
    )
}
