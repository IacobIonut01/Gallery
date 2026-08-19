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
fun CloudNotificationSettingsScreen(configId: Long) {
    val settingsVm = hiltViewModel<CloudSettingsViewModel>()
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()

    LaunchedEffect(configId) { settingsVm.loadConfig(configId) }
    if (config == null) {
        AccountSettingsStateScreen(stringResource(R.string.cloud_notifications), loadState)
        return
    }
    val showTotalProgress = config?.showBackupTotalProgress ?: true
    val showDetailProgress = config?.showBackupDetailProgress ?: false
    val notifyFailures = config?.notifyBackupFailures ?: true
    val backgroundBackupHeader = stringResource(R.string.cloud_notif_background_backup)
    val totalProgressTitle = stringResource(R.string.cloud_notif_total_progress)
    val totalProgressSummary = stringResource(R.string.cloud_notif_total_progress_summary)
    val detailProgressTitle = stringResource(R.string.cloud_notif_detail_progress)
    val detailProgressSummary = stringResource(R.string.cloud_notif_detail_progress_summary)
    val failuresTitle = stringResource(R.string.cloud_notif_failures)
    val failuresSummary = stringResource(R.string.cloud_notif_failures_summary)
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
        title = stringResource(R.string.cloud_notifications),
        settingsList = settingsList
    )
}
