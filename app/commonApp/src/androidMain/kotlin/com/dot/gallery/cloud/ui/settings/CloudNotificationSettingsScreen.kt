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
import com.dot.gallery.generated.resources.cloud_notif_background_backup
import com.dot.gallery.generated.resources.cloud_notif_detail_progress
import com.dot.gallery.generated.resources.cloud_notif_detail_progress_summary
import com.dot.gallery.generated.resources.cloud_notif_failures
import com.dot.gallery.generated.resources.cloud_notif_failures_summary
import com.dot.gallery.generated.resources.cloud_notif_total_progress
import com.dot.gallery.generated.resources.cloud_notif_total_progress_summary
import com.dot.gallery.generated.resources.cloud_notifications


@Composable
fun CloudNotificationSettingsScreen(configId: Long) {
    val settingsVm = injectViewModel<CloudSettingsViewModel> {cloudSettingsViewModel}
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()

    LaunchedEffect(configId) { settingsVm.loadConfig(configId) }
    if (config == null) {
        AccountSettingsStateScreen(stringResource(Res.string.cloud_notifications), loadState)
        return
    }
    val showTotalProgress = config?.showBackupTotalProgress ?: true
    val showDetailProgress = config?.showBackupDetailProgress ?: false
    val notifyFailures = config?.notifyBackupFailures ?: true
    val backgroundBackupHeader = stringResource(Res.string.cloud_notif_background_backup)
    val totalProgressTitle = stringResource(Res.string.cloud_notif_total_progress)
    val totalProgressSummary = stringResource(Res.string.cloud_notif_total_progress_summary)
    val detailProgressTitle = stringResource(Res.string.cloud_notif_detail_progress)
    val detailProgressSummary = stringResource(Res.string.cloud_notif_detail_progress_summary)
    val failuresTitle = stringResource(Res.string.cloud_notif_failures)
    val failuresSummary = stringResource(Res.string.cloud_notif_failures_summary)
    val resourceStrings = listOf(
        backgroundBackupHeader,
        totalProgressTitle,
        totalProgressSummary,
        detailProgressTitle,
        detailProgressSummary,
        failuresTitle,
        failuresSummary,
    )

    val settingsList = remember(
        showTotalProgress,
        showDetailProgress,
        notifyFailures,
        resourceStrings,
    ) {
        buildList {
            add(SettingsEntity.Header(title = backgroundBackupHeader))
            add(
                SettingsEntity.SwitchPreference(
                    title = totalProgressTitle,
                    summary = totalProgressSummary,
                    isChecked = showTotalProgress,
                    onCheck = { settingsVm.updateConfig { copy(showBackupTotalProgress = it) } },
                    screenPosition = Position.Top
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    title = detailProgressTitle,
                    summary = detailProgressSummary,
                    isChecked = showDetailProgress,
                    onCheck = { settingsVm.updateConfig { copy(showBackupDetailProgress = it) } },
                    screenPosition = Position.Middle
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    title = failuresTitle,
                    summary = failuresSummary,
                    isChecked = notifyFailures,
                    onCheck = { settingsVm.updateConfig { copy(notifyBackupFailures = it) } },
                    screenPosition = Position.Bottom
                )
            )
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(Res.string.cloud_notifications),
        settingsList = settingsList
    )
}
