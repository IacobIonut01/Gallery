/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.cloud.ui.settings.AccountSettingsStateScreen
import com.dot.gallery.cloud.ui.settings.CloudSettingsViewModel
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen

@Composable
fun BackupOptionsScreen(configId: Long) {
    val settingsVm = hiltViewModel<CloudSettingsViewModel>()
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()
    val global = configId <= 0L

    LaunchedEffect(configId) { settingsVm.loadConfig(configId, allowGlobal = true) }
    if (config == null) {
        AccountSettingsStateScreen(stringResource(R.string.cloud_backup_options), loadState)
        return
    }

    val cellularPhotos = config?.cellularPhotos ?: false
    val cellularVideos = config?.cellularVideos ?: false
    val requireCharging = config?.requireCharging ?: false
    val syncAlbums = config?.syncAlbums ?: false
    val networkHeader = stringResource(R.string.cloud_backup_network)
    val cellularPhotosTitle = stringResource(R.string.cloud_backup_cellular_photos)
    val cellularPhotosSummary = stringResource(R.string.cloud_backup_cellular_photos_summary)
    val cellularVideosTitle = stringResource(R.string.cloud_backup_cellular_videos)
    val cellularVideosSummary = stringResource(R.string.cloud_backup_cellular_videos_summary)
    val backgroundHeader = stringResource(R.string.cloud_backup_background)
    val requireChargingTitle = stringResource(R.string.cloud_backup_require_charging)
    val requireChargingSummary = stringResource(R.string.cloud_backup_require_charging_summary)
    val albumSyncHeader = stringResource(R.string.cloud_backup_album_sync)
    val syncAlbumsTitle = stringResource(R.string.cloud_backup_sync_albums)
    val syncAlbumsSummary = stringResource(R.string.cloud_backup_sync_albums_summary)
    val resourceStrings = listOf(
        networkHeader,
        cellularPhotosTitle,
        cellularPhotosSummary,
        cellularVideosTitle,
        cellularVideosSummary,
        backgroundHeader,
        requireChargingTitle,
        requireChargingSummary,
        albumSyncHeader,
        syncAlbumsTitle,
        syncAlbumsSummary,
    )

    val settingsList = remember(
        cellularPhotos,
        cellularVideos,
        requireCharging,
        syncAlbums,
        resourceStrings,
    ) {
        buildList {
            // Network section
            add(SettingsEntity.Header(title = networkHeader))
            add(
                SettingsEntity.SwitchPreference(
                    title = cellularPhotosTitle,
                    summary = cellularPhotosSummary,
                    isChecked = cellularPhotos,
                    onCheck = { settingsVm.updateConfig { copy(cellularPhotos = it) } },
                    screenPosition = Position.Top
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    title = cellularVideosTitle,
                    summary = cellularVideosSummary,
                    isChecked = cellularVideos,
                    onCheck = { settingsVm.updateConfig { copy(cellularVideos = it) } },
                    screenPosition = Position.Bottom
                )
            )

            // Background section
            add(SettingsEntity.Header(title = backgroundHeader))
            add(
                SettingsEntity.SwitchPreference(
                    title = requireChargingTitle,
                    summary = requireChargingSummary,
                    isChecked = requireCharging,
                    onCheck = { settingsVm.updateConfig { copy(requireCharging = it) } },
                    screenPosition = Position.Alone
                )
            )

            // Albums section
            add(SettingsEntity.Header(title = albumSyncHeader))
            add(
                SettingsEntity.SwitchPreference(
                    title = syncAlbumsTitle,
                    summary = syncAlbumsSummary,
                    isChecked = syncAlbums,
                    onCheck = { settingsVm.updateConfig { copy(syncAlbums = it) } },
                    screenPosition = Position.Alone
                )
            )
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = if (global) {
            stringResource(R.string.cloud_global) + " · " +
                stringResource(R.string.cloud_backup_options)
        } else {
            stringResource(R.string.cloud_backup_options)
        },
        settingsList = settingsList
    )
}
