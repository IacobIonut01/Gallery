/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.UploadTargetResolver
import com.dot.gallery.cloud.ui.settings.AccountSettingsStateScreen
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.ui.components.BaseSettingsScreen
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.cloud.ui.settings.CloudSettingsViewModel
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.action_cancel
import com.dot.gallery.generated.resources.action_save
import com.dot.gallery.generated.resources.cloud_backup_album_sync
import com.dot.gallery.generated.resources.cloud_backup_background
import com.dot.gallery.generated.resources.cloud_backup_cellular_photos
import com.dot.gallery.generated.resources.cloud_backup_cellular_photos_summary
import com.dot.gallery.generated.resources.cloud_backup_cellular_videos
import com.dot.gallery.generated.resources.cloud_backup_cellular_videos_summary
import com.dot.gallery.generated.resources.cloud_backup_destination
import com.dot.gallery.generated.resources.cloud_backup_folder_dialog_hint
import com.dot.gallery.generated.resources.cloud_backup_network
import com.dot.gallery.generated.resources.cloud_backup_options
import com.dot.gallery.generated.resources.cloud_backup_require_charging
import com.dot.gallery.generated.resources.cloud_backup_require_charging_summary
import com.dot.gallery.generated.resources.cloud_backup_sync_albums
import com.dot.gallery.generated.resources.cloud_backup_sync_albums_summary
import com.dot.gallery.generated.resources.cloud_backup_upload_folder
import com.dot.gallery.generated.resources.cloud_backup_upload_folder_default
import com.dot.gallery.generated.resources.cloud_backup_upload_folder_summary
import com.dot.gallery.generated.resources.cloud_backup_videos_folder
import com.dot.gallery.generated.resources.cloud_backup_videos_folder_same
import com.dot.gallery.generated.resources.cloud_backup_videos_folder_summary
import com.dot.gallery.generated.resources.cloud_global


@Composable
fun BackupOptionsScreen(configId: Long) {
    val settingsVm = injectViewModel<CloudSettingsViewModel> {cloudSettingsViewModel}
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()
    val global = configId <= 0L

    LaunchedEffect(configId) { settingsVm.loadConfig(configId, allowGlobal = true) }
    if (config == null) {
        AccountSettingsStateScreen(stringResource(Res.string.cloud_backup_options), loadState)
        return
    }

    val cellularPhotos = config?.cellularPhotos ?: false
    val cellularVideos = config?.cellularVideos ?: false
    val requireCharging = config?.requireCharging ?: false
    val syncAlbums = config?.syncAlbums ?: false
    // Content-addressed providers (Immich) have no folder semantics; the destination
    // section is only meaningful for path-based stores (WebDAV/ownCloud/Nextcloud/SMB/NFS).
    val supportsUploadPaths = config?.providerType != ProviderType.IMMICH
    val uploadBasePath = config?.uploadBasePath.orEmpty()
    val uploadVideosPath = config?.uploadVideosPath.orEmpty()
    var editingPath by remember { mutableStateOf<PathField?>(null) }

    val networkHeader = stringResource(Res.string.cloud_backup_network)
    val cellularPhotosTitle = stringResource(Res.string.cloud_backup_cellular_photos)
    val cellularPhotosSummary = stringResource(Res.string.cloud_backup_cellular_photos_summary)
    val cellularVideosTitle = stringResource(Res.string.cloud_backup_cellular_videos)
    val cellularVideosSummary = stringResource(Res.string.cloud_backup_cellular_videos_summary)
    val backgroundHeader = stringResource(Res.string.cloud_backup_background)
    val requireChargingTitle = stringResource(Res.string.cloud_backup_require_charging)
    val requireChargingSummary = stringResource(Res.string.cloud_backup_require_charging_summary)
    val albumSyncHeader = stringResource(Res.string.cloud_backup_album_sync)
    val syncAlbumsTitle = stringResource(Res.string.cloud_backup_sync_albums)
    val syncAlbumsSummary = stringResource(Res.string.cloud_backup_sync_albums_summary)
    val destinationHeader = stringResource(Res.string.cloud_backup_destination)
    val uploadFolderTitle = stringResource(Res.string.cloud_backup_upload_folder)
    val uploadFolderSummary = stringResource(Res.string.cloud_backup_upload_folder_summary)
    val uploadFolderDefault = stringResource(Res.string.cloud_backup_upload_folder_default)
    val videosFolderTitle = stringResource(Res.string.cloud_backup_videos_folder)
    val videosFolderSummary = stringResource(Res.string.cloud_backup_videos_folder_summary)
    val videosFolderSame = stringResource(Res.string.cloud_backup_videos_folder_same)
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
        destinationHeader,
        uploadFolderTitle,
        uploadFolderSummary,
        uploadFolderDefault,
        videosFolderTitle,
        videosFolderSummary,
        videosFolderSame,
    )

    val settingsList = remember(
        cellularPhotos,
        cellularVideos,
        requireCharging,
        syncAlbums,
        supportsUploadPaths,
        uploadBasePath,
        uploadVideosPath,
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

            if (supportsUploadPaths) {
                add(SettingsEntity.Header(title = destinationHeader))
                add(
                    SettingsEntity.Preference(
                        title = uploadFolderTitle,
                        summary = uploadFolderSummary,
                        rightText = uploadBasePath.ifBlank { uploadFolderDefault },
                        onClick = { editingPath = PathField.BASE },
                        screenPosition = Position.Top
                    )
                )
                add(
                    SettingsEntity.Preference(
                        title = videosFolderTitle,
                        summary = videosFolderSummary,
                        rightText = uploadVideosPath.ifBlank { videosFolderSame },
                        onClick = { editingPath = PathField.VIDEOS },
                        screenPosition = Position.Bottom
                    )
                )
            }
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = if (global) {
            stringResource(Res.string.cloud_global) + " · " +
                stringResource(Res.string.cloud_backup_options)
        } else {
            stringResource(Res.string.cloud_backup_options)
        },
        settingsList = settingsList
    )

    editingPath?.let { field ->
        val isVideos = field == PathField.VIDEOS
        var text by remember(field) {
            mutableStateOf(if (isVideos) uploadVideosPath else uploadBasePath)
        }
        AlertDialog(
            onDismissRequest = { editingPath = null },
            title = { Text(if (isVideos) videosFolderTitle else uploadFolderTitle) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(Res.string.cloud_backup_folder_dialog_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val sanitized = UploadTargetResolver.sanitizePath(text)
                    settingsVm.updateConfig {
                        if (isVideos) copy(uploadVideosPath = sanitized) else copy(uploadBasePath = sanitized)
                    }
                    editingPath = null
                }) { Text(stringResource(Res.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingPath = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}

private enum class PathField { BASE, VIDEOS }
