/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.text.format.Formatter
import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.data.dao.PendingDownloadInfo
import com.dot.gallery.data.entity.CloudServerConfigEntity
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.navigate
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.components.BaseSettingsScreen
import com.dot.gallery.core.presentation.util.Screen
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.action_cancel
import com.dot.gallery.generated.resources.cloud_adv_troubleshooting
import com.dot.gallery.generated.resources.cloud_advanced
import com.dot.gallery.generated.resources.cloud_authenticating
import com.dot.gallery.generated.resources.cloud_auto_sync
import com.dot.gallery.generated.resources.cloud_auto_sync_interval
import com.dot.gallery.generated.resources.cloud_auto_sync_summary
import com.dot.gallery.generated.resources.cloud_backup_albums
import com.dot.gallery.generated.resources.cloud_backup_options
import com.dot.gallery.generated.resources.cloud_backup_options_summary
import com.dot.gallery.generated.resources.cloud_backup_select_albums
import com.dot.gallery.generated.resources.cloud_connected
import com.dot.gallery.generated.resources.cloud_connection_header
import com.dot.gallery.generated.resources.cloud_credentials
import com.dot.gallery.generated.resources.cloud_credentials_summary
import com.dot.gallery.generated.resources.cloud_delete
import com.dot.gallery.generated.resources.cloud_delete_confirm
import com.dot.gallery.generated.resources.cloud_delete_confirm_action
import com.dot.gallery.generated.resources.cloud_delete_confirm_title
import com.dot.gallery.generated.resources.cloud_delete_failed
import com.dot.gallery.generated.resources.cloud_delete_in_progress
import com.dot.gallery.generated.resources.cloud_delete_provider
import com.dot.gallery.generated.resources.cloud_disconnected
import com.dot.gallery.generated.resources.cloud_download_remote
import com.dot.gallery.generated.resources.cloud_download_remote_confirm
import com.dot.gallery.generated.resources.cloud_download_remote_confirm_loading
import com.dot.gallery.generated.resources.cloud_download_remote_summary
import com.dot.gallery.generated.resources.cloud_download_videos
import com.dot.gallery.generated.resources.cloud_download_videos_summary
import com.dot.gallery.generated.resources.cloud_error
import com.dot.gallery.generated.resources.cloud_free_space
import com.dot.gallery.generated.resources.cloud_free_space_description
import com.dot.gallery.generated.resources.cloud_net_auto_url_summary
import com.dot.gallery.generated.resources.cloud_networking
import com.dot.gallery.generated.resources.cloud_notif_background_backup
import com.dot.gallery.generated.resources.cloud_notifications
import com.dot.gallery.generated.resources.cloud_offline_summary
import com.dot.gallery.generated.resources.cloud_offline_title
import com.dot.gallery.generated.resources.cloud_profile_storage
import com.dot.gallery.generated.resources.cloud_provider_settings
import com.dot.gallery.generated.resources.cloud_remove_downloaded_copies
import com.dot.gallery.generated.resources.cloud_remove_downloaded_copies_confirm
import com.dot.gallery.generated.resources.cloud_remove_downloaded_copies_removing
import com.dot.gallery.generated.resources.cloud_remove_downloaded_copies_summary
import com.dot.gallery.generated.resources.cloud_retry
import com.dot.gallery.generated.resources.cloud_server_url_label
import com.dot.gallery.generated.resources.cloud_server_version_label
import com.dot.gallery.generated.resources.cloud_settings_more_header
import com.dot.gallery.generated.resources.cloud_storage_used
import com.dot.gallery.generated.resources.cloud_sync_header
import com.dot.gallery.generated.resources.cloud_sync_interval_12hr
import com.dot.gallery.generated.resources.cloud_sync_interval_15min
import com.dot.gallery.generated.resources.cloud_sync_interval_1hr
import com.dot.gallery.generated.resources.cloud_sync_interval_24hr
import com.dot.gallery.generated.resources.cloud_sync_interval_30min
import com.dot.gallery.generated.resources.cloud_sync_interval_6hr
import com.dot.gallery.generated.resources.cloud_sync_now
import com.dot.gallery.generated.resources.cloud_sync_now_summary
import com.dot.gallery.generated.resources.cloud_sync_now_syncing
import com.dot.gallery.generated.resources.cloud_sync_wifi_only
import com.dot.gallery.generated.resources.cloud_syncing
import com.dot.gallery.generated.resources.cloud_viewer
import com.dot.gallery.generated.resources.cloud_viewer_load_preview
import com.dot.gallery.generated.resources.cloud_wifi_only_summary


/**
 * Stateless per-account settings surface: the header card, the Sync/downloads/
 * backup/connection/more sections and all four dialogs (interval, download
 * confirmation, remove-copies, delete account). [CloudProviderSettingsScreen]
 * collects the ViewModel state and delegates here; the docs capture tier feeds
 * a sample account instead.
 *
 * [initialDialog] only seeds the existing remember flags — the production
 * default NONE reproduces the screen exactly as before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudProviderSettingsContent(
    state: CloudProviderSettingsContentState,
    actions: CloudProviderSettingsContentActions,
    initialDialog: CloudProviderSettingsDialog = CloudProviderSettingsDialog.NONE,
) {
    val config = state.config
    val eventHandler = LocalEventHandler.current
    val context = LocalContext.current
    var showDeleteDialog by rememberSaveable(config.id) {
        mutableStateOf(initialDialog == CloudProviderSettingsDialog.DELETE_ACCOUNT)
    }
    var showIntervalDialog by remember {
        mutableStateOf(initialDialog == CloudProviderSettingsDialog.SYNC_INTERVAL)
    }
    var showDownloadEnableDialog by rememberSaveable(config.id) {
        mutableStateOf(initialDialog == CloudProviderSettingsDialog.DOWNLOAD)
    }
    var showRemoveCopiesDialog by rememberSaveable(config.id) {
        mutableStateOf(initialDialog == CloudProviderSettingsDialog.REMOVE_DOWNLOADS)
    }

    if (showDeleteDialog || state.deleting) {
        AlertDialog(
            onDismissRequest = { if (!state.deleting) showDeleteDialog = false },
            title = { Text(stringResource(Res.string.cloud_delete_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(Res.string.cloud_delete_confirm))
                    if (state.deleting) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                            Text(stringResource(Res.string.cloud_delete_in_progress))
                        }
                    } else if (state.deletionFailed) {
                        Text(
                            text = stringResource(Res.string.cloud_delete_failed),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.deleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = { actions.deleteServer() }
                ) {
                    Text(
                        stringResource(
                            if (state.deletionFailed) Res.string.cloud_retry else Res.string.cloud_delete_confirm_action
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.deleting,
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }

    val connState = state.connectionState
    val storage = state.storage
    val version = state.serverVersion
    val syncProgress = state.syncProgress
    val isSyncing = syncProgress?.isSyncing == true
    val syncHeader = stringResource(Res.string.cloud_sync_header)
    val syncNowSyncingTitle = stringResource(Res.string.cloud_sync_now_syncing)
    val syncNowTitle = stringResource(Res.string.cloud_sync_now)
    val syncNowSummary = stringResource(Res.string.cloud_sync_now_summary)
    val autoSyncTitle = stringResource(Res.string.cloud_auto_sync)
    val autoSyncSummary = stringResource(Res.string.cloud_auto_sync_summary)
    val autoSyncIntervalTitle = stringResource(Res.string.cloud_auto_sync_interval)
    val syncIntervalSummary = syncIntervalLabel(config.syncIntervalMinutes)
    val wifiOnlyTitle = stringResource(Res.string.cloud_sync_wifi_only)
    val wifiOnlySummary = stringResource(Res.string.cloud_wifi_only_summary)
    val downloadRemoteTitle = stringResource(Res.string.cloud_download_remote)
    val downloadRemoteSummary = stringResource(Res.string.cloud_download_remote_summary)
    val downloadVideosTitle = stringResource(Res.string.cloud_download_videos)
    val downloadVideosSummary = stringResource(Res.string.cloud_download_videos_summary)
    val backupAlbumsTitle = stringResource(Res.string.cloud_backup_albums)
    val selectAlbumsSummary = stringResource(Res.string.cloud_backup_select_albums)
    val backupOptionsTitle = stringResource(Res.string.cloud_backup_options)
    val backupOptionsSummary = stringResource(Res.string.cloud_backup_options_summary)
    val connectionHeader = stringResource(Res.string.cloud_connection_header)
    val credentialsTitle = stringResource(Res.string.cloud_credentials)
    val credentialsSummary = stringResource(Res.string.cloud_credentials_summary)
    val networkingTitle = stringResource(Res.string.cloud_networking)
    val networkingSummary = stringResource(Res.string.cloud_net_auto_url_summary)
    val freeSpaceTitle = stringResource(Res.string.cloud_free_space)
    val freeSpaceSummary = stringResource(Res.string.cloud_free_space_description)
    val moreHeader = stringResource(Res.string.cloud_settings_more_header)
    val notificationsTitle = stringResource(Res.string.cloud_notifications)
    val notificationsSummary = stringResource(Res.string.cloud_notif_background_backup)
    val viewerTitle = stringResource(Res.string.cloud_viewer)
    val viewerSummary = stringResource(Res.string.cloud_viewer_load_preview)
    val offlineTitle = stringResource(Res.string.cloud_offline_title)
    val offlineSummary = stringResource(Res.string.cloud_offline_summary)
    val advancedTitle = stringResource(Res.string.cloud_advanced)
    val advancedSummary = stringResource(Res.string.cloud_adv_troubleshooting)
    val copiesInfo = state.copiesInfo
    val copiesCount = copiesInfo?.itemCount ?: 0
    val isRemovingCopies = state.removingCopies
    val showRemoveRow = copiesCount > 0 || isRemovingCopies
    val removeCopiesTitle = stringResource(Res.string.cloud_remove_downloaded_copies)
    val removeCopiesSummary = if (isRemovingCopies) {
        stringResource(Res.string.cloud_remove_downloaded_copies_removing)
    } else {
        stringResource(
            Res.string.cloud_remove_downloaded_copies_summary,
            copiesCount,
            Formatter.formatFileSize(context, copiesInfo?.totalBytes ?: 0L)
        )
    }
    val resourceStrings = listOf(
        syncHeader,
        syncNowSyncingTitle,
        syncNowTitle,
        syncNowSummary,
        autoSyncTitle,
        autoSyncSummary,
        autoSyncIntervalTitle,
        syncIntervalSummary,
        wifiOnlyTitle,
        wifiOnlySummary,
        downloadRemoteTitle,
        downloadRemoteSummary,
        downloadVideosTitle,
        downloadVideosSummary,
        backupAlbumsTitle,
        selectAlbumsSummary,
        backupOptionsTitle,
        backupOptionsSummary,
        connectionHeader,
        credentialsTitle,
        credentialsSummary,
        networkingTitle,
        networkingSummary,
        freeSpaceTitle,
        freeSpaceSummary,
        moreHeader,
        notificationsTitle,
        notificationsSummary,
        viewerTitle,
        viewerSummary,
        offlineTitle,
        offlineSummary,
        advancedTitle,
        advancedSummary,
        removeCopiesTitle,
        removeCopiesSummary,
    )

    val settingsList = remember(
        config, isSyncing, connState, version, resourceStrings,
        copiesCount, isRemovingCopies, showRemoveRow
    ) {
        val items = mutableStateListOf<SettingsEntity>()

        // Sync section
        items.add(SettingsEntity.Header(title = syncHeader))

        items.add(
            SettingsEntity.Preference(
                title = if (isSyncing) syncNowSyncingTitle else syncNowTitle,
                summary = if (isSyncing) (syncProgress.message ?: "") else syncNowSummary,
                enabled = !isSyncing,
                onClick = { actions.syncNow() },
                screenPosition = Position.Top
            )
        )
        items.add(
            SettingsEntity.SwitchPreference(
                title = autoSyncTitle,
                summary = autoSyncSummary,
                isChecked = config.syncEnabled,
                onCheck = { checked ->
                    actions.updateConfig { copy(syncEnabled = checked) }
                },
                screenPosition = if (config.syncEnabled) Position.Middle else Position.Bottom
            )
        )
        if (config.syncEnabled) {
            items.add(
                SettingsEntity.Preference(
                    title = autoSyncIntervalTitle,
                    summary = syncIntervalSummary,
                    onClick = { showIntervalDialog = true },
                    screenPosition = Position.Middle
                )
            )
            items.add(
                SettingsEntity.SwitchPreference(
                    title = wifiOnlyTitle,
                    summary = wifiOnlySummary,
                    isChecked = config.wifiOnly,
                    onCheck = { checked ->
                        actions.updateConfig { copy(wifiOnly = checked) }
                    },
                    screenPosition = Position.Bottom
                )
            )
        }

        // Remote -> local downloads
        items.add(
            SettingsEntity.SwitchPreference(
                title = downloadRemoteTitle,
                summary = downloadRemoteSummary,
                isChecked = config.downloadRemoteEnabled,
                onCheck = { checked ->
                    if (checked) {
                        showDownloadEnableDialog = true
                    } else {
                        actions.updateConfig { copy(downloadRemoteEnabled = false) }
                    }
                },
                screenPosition = if (config.downloadRemoteEnabled || showRemoveRow) {
                    Position.Top
                } else {
                    Position.Alone
                }
            )
        )
        if (config.downloadRemoteEnabled) {
            items.add(
                SettingsEntity.SwitchPreference(
                    title = downloadVideosTitle,
                    summary = downloadVideosSummary,
                    isChecked = config.downloadVideos,
                    onCheck = { checked ->
                        actions.updateConfig { copy(downloadVideos = checked) }
                    },
                    screenPosition = if (showRemoveRow) Position.Middle else Position.Bottom
                )
            )
        }
        if (showRemoveRow) {
            items.add(
                SettingsEntity.Preference(
                    title = removeCopiesTitle,
                    summary = removeCopiesSummary,
                    enabled = copiesCount > 0 && !isRemovingCopies,
                    onClick = { showRemoveCopiesDialog = true },
                    screenPosition = Position.Bottom
                )
            )
        }

        // Backup albums for THIS account
        items.add(
            SettingsEntity.Preference(
                title = backupAlbumsTitle,
                summary = selectAlbumsSummary,
                onClick = {
                    eventHandler.navigate(
                        Screen.CloudUploadSettingsScreen.configId(config.id)
                    )
                },
                screenPosition = Position.Top
            )
        )
        items.add(
            SettingsEntity.Preference(
                title = backupOptionsTitle,
                summary = backupOptionsSummary,
                onClick = {
                    eventHandler.navigate(Screen.BackupOptionsScreen.configId(config.id))
                },
                screenPosition = Position.Bottom
            )
        )

        // Connection section
        items.add(SettingsEntity.Header(title = connectionHeader))
        items.add(
            SettingsEntity.Preference(
                title = credentialsTitle,
                summary = credentialsSummary,
                onClick = {
                    eventHandler.navigate(
                        Screen.CloudEditServerScreen.configId(config.id)
                    )
                },
                screenPosition = Position.Top
            )
        )
        items.add(
            SettingsEntity.Preference(
                title = networkingTitle,
                summary = networkingSummary,
                onClick = { eventHandler.navigate(Screen.CloudNetworkingScreen.configId(config.id)) },
                screenPosition = Position.Bottom
            )
        )

        // Features section
        items.add(SettingsEntity.Header(title = freeSpaceTitle))
        items.add(
            SettingsEntity.Preference(
                title = freeSpaceTitle,
                summary = freeSpaceSummary,
                onClick = { eventHandler.navigate(Screen.FreeUpSpaceScreen.configId(config.id)) },
                screenPosition = Position.Alone
            )
        )

        // Settings section
        items.add(SettingsEntity.Header(title = moreHeader))
        items.add(
            SettingsEntity.Preference(
                title = notificationsTitle,
                summary = notificationsSummary,
                onClick = {
                    eventHandler.navigate(Screen.CloudNotificationSettingsScreen.configId(config.id))
                },
                screenPosition = Position.Top
            )
        )
        items.add(
            SettingsEntity.Preference(
                title = viewerTitle,
                summary = viewerSummary,
                onClick = { eventHandler.navigate(Screen.CloudViewerSettingsScreen.configId(config.id)) },
                screenPosition = Position.Middle
            )
        )
        items.add(
            SettingsEntity.Preference(
                title = offlineTitle,
                summary = offlineSummary,
                onClick = { eventHandler.navigate(Screen.CloudOfflineModeScreen()) },
                screenPosition = Position.Middle
            )
        )
        items.add(
            SettingsEntity.Preference(
                title = advancedTitle,
                summary = advancedSummary,
                onClick = { eventHandler.navigate(Screen.CloudAdvancedSettingsScreen.configId(config.id)) },
                screenPosition = Position.Bottom
            )
        )

        items
    }

    BaseSettingsScreen(
        title = stringResource(Res.string.cloud_provider_settings),
        topContent = {
            ProviderSettingsHeader(
                config = config,
                connState = connState,
                storage = storage,
                version = version
            )
        },
        settingsList = settingsList,
        bottomContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SetupButton(
                    text = stringResource(Res.string.cloud_delete_provider),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    onClick = { showDeleteDialog = true }
                )
            }
        }
    )

    if (showIntervalDialog) {
        SyncIntervalDialog(
            currentMinutes = config.syncIntervalMinutes,
            onDismiss = { showIntervalDialog = false },
            onSelect = { minutes ->
                showIntervalDialog = false
                actions.updateConfig { copy(syncIntervalMinutes = minutes) }
            }
        )
    }

    if (showDownloadEnableDialog) {
        val stats by produceState<PendingDownloadInfo?>(initialValue = null, config.id) {
            value = actions.pendingDownloadStats()
        }
        AlertDialog(
            onDismissRequest = { showDownloadEnableDialog = false },
            title = { Text(downloadRemoteTitle) },
            text = {
                Text(
                    stats?.let {
                        stringResource(
                            Res.string.cloud_download_remote_confirm,
                            it.itemCount,
                            Formatter.formatFileSize(context, it.totalBytes)
                        )
                    } ?: stringResource(Res.string.cloud_download_remote_confirm_loading)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDownloadEnableDialog = false
                        actions.updateConfig { copy(downloadRemoteEnabled = true) }
                        actions.triggerDownload()
                    }
                ) {
                    Text(downloadRemoteTitle)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadEnableDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }

    if (showRemoveCopiesDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveCopiesDialog = false },
            title = { Text(removeCopiesTitle) },
            text = {
                Text(
                    stringResource(
                        Res.string.cloud_remove_downloaded_copies_confirm,
                        copiesCount,
                        Formatter.formatFileSize(context, copiesInfo?.totalBytes ?: 0L)
                    )
                )
            },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        actions.removeDownloadedCopies()
                        showRemoveCopiesDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.cloud_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveCopiesDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ProviderSettingsHeader(
    config: CloudServerConfigEntity,
    connState: ConnectionState,
    storage: CloudStorageInfo?,
    version: String?
) {
    val stateText = when (connState) {
        ConnectionState.CONNECTED -> stringResource(Res.string.cloud_connected)
        ConnectionState.DISCONNECTED -> stringResource(Res.string.cloud_disconnected)
        ConnectionState.SYNCING -> stringResource(Res.string.cloud_syncing)
        ConnectionState.AUTHENTICATING -> stringResource(Res.string.cloud_authenticating)
        ConnectionState.ERROR -> stringResource(Res.string.cloud_error)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(all = 20.dp)
        ) {
            // Provider icon + name + status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.displayName.ifBlank { config.providerType.displayName },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Server Storage section — only shown when the provider reports usage.
            if (storage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.cloud_profile_storage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                SettingsStorageBar(storage = storage)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        Res.string.cloud_storage_used,
                        storage.usedFormatted,
                        storage.totalFormatted
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Info rows inside the card. Server version is only shown when the
            // provider actually reports one; the URL is always shown.
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))
            if (!version.isNullOrBlank()) {
                InfoRow(
                    label = stringResource(Res.string.cloud_server_version_label),
                    value = version
                )
                Spacer(Modifier.height(8.dp))
            }
            InfoRow(
                label = stringResource(Res.string.cloud_server_url_label),
                value = config.serverUrl
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    // Label keeps its intrinsic width; the value takes the remaining space and is
    // end-aligned so long values (e.g. server URLs) wrap onto their own lines
    // instead of overlapping the label.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsStorageBar(storage: CloudStorageInfo?) {
    val fraction = ((storage?.usedPercentage ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600),
        label = "storageFraction"
    )
    val pct = storage?.usedPercentage ?: 0.0
    val progressColor = when {
        pct > 90 -> MaterialTheme.colorScheme.error
        pct > 75 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val barHeight = 8.dp
    val barCornerRadius = 4.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(barCornerRadius))
            .drawWithCache {
                onDrawBehind {
                    drawRoundRect(
                        color = trackColor,
                        cornerRadius = CornerRadius(barCornerRadius.toPx(), barCornerRadius.toPx())
                    )
                    drawRoundRect(
                        color = progressColor,
                        size = Size(size.width * animatedFraction, size.height)
                    )
                }
            }
    )
}

private val syncIntervalOptions = listOf(15, 30, 60, 360, 720, 1440)

@Composable
private fun syncIntervalLabel(minutes: Int): String = when (minutes) {
    15 -> stringResource(Res.string.cloud_sync_interval_15min)
    30 -> stringResource(Res.string.cloud_sync_interval_30min)
    60 -> stringResource(Res.string.cloud_sync_interval_1hr)
    360 -> stringResource(Res.string.cloud_sync_interval_6hr)
    720 -> stringResource(Res.string.cloud_sync_interval_12hr)
    1440 -> stringResource(Res.string.cloud_sync_interval_24hr)
    else -> stringResource(Res.string.cloud_sync_interval_6hr)
}

@Composable
private fun SyncIntervalDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.cloud_auto_sync_interval)) },
        text = {
            Column {
                syncIntervalOptions.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (minutes == currentMinutes)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .clickable { onSelect(minutes) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = syncIntervalLabel(minutes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (minutes == currentMinutes)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}
