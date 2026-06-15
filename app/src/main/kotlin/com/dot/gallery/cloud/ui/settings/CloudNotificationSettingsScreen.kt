/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen

@Composable
fun CloudNotificationSettingsScreen() {
    val settingsVm = hiltViewModel<CloudSettingsViewModel>()
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val showTotalProgress = config?.showBackupTotalProgress ?: true
    val showDetailProgress = config?.showBackupDetailProgress ?: false
    val notifyFailures = config?.notifyBackupFailures ?: true

    val settingsList = remember(showTotalProgress, showDetailProgress, notifyFailures) {
        buildList {
            add(SettingsEntity.Header(title = context.getString(R.string.cloud_notif_background_backup)))
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_notif_total_progress),
                    summary = context.getString(R.string.cloud_notif_total_progress_summary),
                    isChecked = showTotalProgress,
                    onCheck = { settingsVm.updateConfig { copy(showBackupTotalProgress = it) } },
                    screenPosition = Position.Top
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_notif_detail_progress),
                    summary = context.getString(R.string.cloud_notif_detail_progress_summary),
                    isChecked = showDetailProgress,
                    onCheck = { settingsVm.updateConfig { copy(showBackupDetailProgress = it) } },
                    screenPosition = Position.Middle
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_notif_failures),
                    summary = context.getString(R.string.cloud_notif_failures_summary),
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
