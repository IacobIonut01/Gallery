/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.cloud.ui.settings.CloudSettingsViewModel
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen

@Composable
fun BackupOptionsScreen() {
    val settingsVm = hiltViewModel<CloudSettingsViewModel>()
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val cellularPhotos = config?.cellularPhotos ?: false
    val cellularVideos = config?.cellularVideos ?: false
    val requireCharging = config?.requireCharging ?: false
    val syncAlbums = config?.syncAlbums ?: false
    val context = LocalContext.current

    val settingsList = remember(cellularPhotos, cellularVideos, requireCharging, syncAlbums) {
        buildList {
            // Network section
            add(SettingsEntity.Header(title = context.getString(R.string.cloud_backup_network)))
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_backup_cellular_photos),
                    summary = context.getString(R.string.cloud_backup_cellular_photos_summary),
                    isChecked = cellularPhotos,
                    onCheck = { settingsVm.updateConfig { copy(cellularPhotos = it) } },
                    screenPosition = Position.Top
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_backup_cellular_videos),
                    summary = context.getString(R.string.cloud_backup_cellular_videos_summary),
                    isChecked = cellularVideos,
                    onCheck = { settingsVm.updateConfig { copy(cellularVideos = it) } },
                    screenPosition = Position.Bottom
                )
            )

            // Background section
            add(SettingsEntity.Header(title = context.getString(R.string.cloud_backup_background)))
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_backup_require_charging),
                    summary = context.getString(R.string.cloud_backup_require_charging_summary),
                    isChecked = requireCharging,
                    onCheck = { settingsVm.updateConfig { copy(requireCharging = it) } },
                    screenPosition = Position.Alone
                )
            )

            // Albums section
            add(SettingsEntity.Header(title = context.getString(R.string.cloud_backup_album_sync)))
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_backup_sync_albums),
                    summary = context.getString(R.string.cloud_backup_sync_albums_summary),
                    isChecked = syncAlbums,
                    onCheck = { settingsVm.updateConfig { copy(syncAlbums = it) } },
                    screenPosition = Position.Alone
                )
            )
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(R.string.cloud_backup_options),
        settingsList = settingsList
    )
}
