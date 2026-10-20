/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.offline

import android.text.format.Formatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.components.BaseSettingsScreen
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.core.presentation.components.shape
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_offline_auto
import com.dot.gallery.generated.resources.cloud_offline_auto_only
import com.dot.gallery.generated.resources.cloud_offline_automatic_cache
import com.dot.gallery.generated.resources.cloud_offline_available_header
import com.dot.gallery.generated.resources.cloud_offline_by_album
import com.dot.gallery.generated.resources.cloud_offline_cache_browsing
import com.dot.gallery.generated.resources.cloud_offline_cache_browsing_summary
import com.dot.gallery.generated.resources.cloud_offline_cache_limit
import com.dot.gallery.generated.resources.cloud_offline_cache_limit_summary
import com.dot.gallery.generated.resources.cloud_offline_cached_by_account
import com.dot.gallery.generated.resources.cloud_offline_cached_content_note
import com.dot.gallery.generated.resources.cloud_offline_cached_size
import com.dot.gallery.generated.resources.cloud_offline_clear
import com.dot.gallery.generated.resources.cloud_offline_clear_account
import com.dot.gallery.generated.resources.cloud_offline_clear_all
import com.dot.gallery.generated.resources.cloud_offline_clear_data
import com.dot.gallery.generated.resources.cloud_offline_clear_message
import com.dot.gallery.generated.resources.cloud_offline_clear_title
import com.dot.gallery.generated.resources.cloud_offline_done
import com.dot.gallery.generated.resources.cloud_offline_download_now
import com.dot.gallery.generated.resources.cloud_offline_download_progress
import com.dot.gallery.generated.resources.cloud_offline_downloading
import com.dot.gallery.generated.resources.cloud_offline_force
import com.dot.gallery.generated.resources.cloud_offline_force_summary
import com.dot.gallery.generated.resources.cloud_offline_free
import com.dot.gallery.generated.resources.cloud_offline_keep_cached
import com.dot.gallery.generated.resources.cloud_offline_loading_albums
import com.dot.gallery.generated.resources.cloud_offline_manage_account
import com.dot.gallery.generated.resources.cloud_offline_megabytes
import com.dot.gallery.generated.resources.cloud_offline_mode_header
import com.dot.gallery.generated.resources.cloud_offline_no_albums
import com.dot.gallery.generated.resources.cloud_offline_no_network
import com.dot.gallery.generated.resources.cloud_offline_pinned
import com.dot.gallery.generated.resources.cloud_offline_remove_downloads
import com.dot.gallery.generated.resources.cloud_offline_status_complete
import com.dot.gallery.generated.resources.cloud_offline_status_downloading
import com.dot.gallery.generated.resources.cloud_offline_status_failed
import com.dot.gallery.generated.resources.cloud_offline_status_not_pinned
import com.dot.gallery.generated.resources.cloud_offline_status_partial
import com.dot.gallery.generated.resources.cloud_offline_status_pinned
import com.dot.gallery.generated.resources.cloud_offline_status_queued
import com.dot.gallery.generated.resources.cloud_offline_title
import com.dot.gallery.generated.resources.cloud_offline_unpin_message
import com.dot.gallery.generated.resources.cloud_offline_unpin_title
import com.dot.gallery.generated.resources.cloud_offline_download_favorites
import com.dot.gallery.generated.resources.cloud_offline_download_favorites_summary
import com.dot.gallery.generated.resources.cloud_offline_wifi_only
import com.dot.gallery.generated.resources.cloud_offline_wifi_only_summary
import com.dot.gallery.generated.resources.gb



@Composable
fun OfflineModeContent(
    state: OfflineModeUiState,
    accountSheet: AccountCacheSheetState?,
    actions: OfflineModeContentActions,
    initialDialog: OfflineModeDialog = OfflineModeDialog.NONE,
) {
    val context = LocalContext.current
    var showBudgetDialog by remember { mutableStateOf(initialDialog == OfflineModeDialog.BUDGET) }
    var showClearDialog by remember { mutableStateOf(initialDialog == OfflineModeDialog.CLEAR) }
    var accountToUnpin by remember { mutableStateOf<OfflineAccount?>(null) }

    val offlineModeHeader = stringResource(Res.string.cloud_offline_mode_header)
    val forceOfflineTitle = stringResource(Res.string.cloud_offline_force)
    val forceOfflineSummary = stringResource(Res.string.cloud_offline_force_summary)
    val noNetworkSummary = stringResource(Res.string.cloud_offline_no_network)
    val automaticCacheHeader = stringResource(Res.string.cloud_offline_automatic_cache)
    val cacheBrowsingTitle = stringResource(Res.string.cloud_offline_cache_browsing)
    val cacheBrowsingSummary = stringResource(Res.string.cloud_offline_cache_browsing_summary)
    val wifiOnlyTitle = stringResource(Res.string.cloud_offline_wifi_only)
    val wifiOnlySummary = stringResource(Res.string.cloud_offline_wifi_only_summary)
    val downloadFavoritesTitle = stringResource(Res.string.cloud_offline_download_favorites)
    val downloadFavoritesSummary = stringResource(Res.string.cloud_offline_download_favorites_summary)
    val cacheLimitTitle = stringResource(Res.string.cloud_offline_cache_limit)
    val cacheLimitSummary = stringResource(Res.string.cloud_offline_cache_limit_summary)
    val availableOfflineHeader = stringResource(Res.string.cloud_offline_available_header)
    val cachedByAccountHeader = stringResource(Res.string.cloud_offline_cached_by_account)
    val budgetText = stringResource(Res.string.cloud_offline_megabytes, state.budgetMb)
    val accountStatusSummaries = mutableMapOf<Long, String>()
    val accountManagementSummaries = mutableMapOf<Long, String>()
    state.accounts.forEach { account ->
        accountStatusSummaries[account.configId] = when (account.availabilityStatus) {
            OfflineAvailabilityStatus.NOT_PINNED ->
                stringResource(Res.string.cloud_offline_status_not_pinned)
            OfflineAvailabilityStatus.PINNED ->
                stringResource(Res.string.cloud_offline_status_pinned)
            OfflineAvailabilityStatus.QUEUED ->
                stringResource(Res.string.cloud_offline_status_queued)
            OfflineAvailabilityStatus.DOWNLOADING ->
                stringResource(Res.string.cloud_offline_status_downloading)
            OfflineAvailabilityStatus.PARTIAL ->
                stringResource(
                    Res.string.cloud_offline_status_partial,
                    account.downloadedVariants,
                    account.totalVariants,
                )
            OfflineAvailabilityStatus.COMPLETE ->
                stringResource(
                    Res.string.cloud_offline_status_complete,
                    account.downloadedVariants,
                    account.totalVariants,
                )
            OfflineAvailabilityStatus.FAILED ->
                stringResource(
                    Res.string.cloud_offline_status_failed,
                    account.downloadedVariants,
                    account.totalVariants,
                )
        }
        accountManagementSummaries[account.configId] = stringResource(
            Res.string.cloud_offline_manage_account,
            account.providerType.displayName,
        )
    }

    val items: SnapshotStateList<SettingsEntity> = remember(
        state,
        offlineModeHeader,
        forceOfflineTitle,
        forceOfflineSummary,
        noNetworkSummary,
        automaticCacheHeader,
        cacheBrowsingTitle,
        cacheBrowsingSummary,
        wifiOnlyTitle,
        wifiOnlySummary,
        downloadFavoritesTitle,
        downloadFavoritesSummary,
        cacheLimitTitle,
        cacheLimitSummary,
        availableOfflineHeader,
        cachedByAccountHeader,
        budgetText,
        accountStatusSummaries,
        accountManagementSummaries,
        context,
    ) {
        buildList {
            // === Offline mode ===
            add(SettingsEntity.Header(title = offlineModeHeader))
            add(
                SettingsEntity.SwitchPreference(
                    icon = Icons.Outlined.CloudOff,
                    title = forceOfflineTitle,
                    summary = if (!state.connected) noNetworkSummary else forceOfflineSummary,
                    isChecked = state.forceOffline,
                    onCheck = { actions.setForceOffline(it) },
                    screenPosition = Position.Alone
                )
            )

            // === Cache-on-view ===
            add(SettingsEntity.Header(title = automaticCacheHeader))
            add(
                SettingsEntity.SwitchPreference(
                    icon = Icons.Outlined.Sync,
                    title = cacheBrowsingTitle,
                    summary = cacheBrowsingSummary,
                    isChecked = state.cacheOnView,
                    onCheck = { actions.setCacheOnView(it) },
                    screenPosition = Position.Top
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    icon = Icons.Outlined.Wifi,
                    title = wifiOnlyTitle,
                    summary = wifiOnlySummary,
                    isChecked = state.cacheWifiOnly,
                    onCheck = { actions.setCacheWifiOnly(it) },
                    screenPosition = Position.Middle
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    icon = Icons.Outlined.CloudDownload,
                    title = downloadFavoritesTitle,
                    summary = downloadFavoritesSummary,
                    isChecked = state.downloadFavoritesFullRes,
                    onCheck = { actions.setDownloadFavoritesFullRes(it) },
                    screenPosition = Position.Middle
                )
            )
            add(
                SettingsEntity.Preference(
                    icon = Icons.Outlined.SdStorage,
                    title = cacheLimitTitle,
                    summary = cacheLimitSummary,
                    rightText = budgetText,
                    onClick = { showBudgetDialog = true },
                    screenPosition = Position.Bottom
                )
            )

            // === Per-account offline ===
            if (state.accounts.isNotEmpty()) {
                add(SettingsEntity.Header(title = availableOfflineHeader))
                state.accounts.forEachIndexed { index, account ->
                    add(
                        SettingsEntity.SwitchPreference(
                            icon = Icons.Outlined.CloudDownload,
                            title = account.label,
                            summary = accountStatusSummaries.getValue(account.configId),
                            isChecked = account.pinned,
                            onCheck = { enabled ->
                                if (enabled) actions.pinAccount(account.configId, account.label)
                                else accountToUnpin = account
                            },
                            screenPosition = positionFor(index, state.accounts.size)
                        )
                    )
                }

                // === Per-account cached-data management ===
                add(SettingsEntity.Header(title = cachedByAccountHeader))
                state.accounts.forEachIndexed { index, account ->
                    add(
                        SettingsEntity.Preference(
                            icon = Icons.Outlined.Storage,
                            title = account.label,
                            summary = accountManagementSummaries.getValue(account.configId),
                            rightText = Formatter.formatShortFileSize(context, account.cacheBytes),
                            onClick = { actions.openAccountCache(account.configId, account.label) },
                            screenPosition = positionFor(index, state.accounts.size)
                        )
                    )
                }
            }
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(Res.string.cloud_offline_title),
        topContent = {
            StorageUsageCard(
                autoBytes = state.autoCacheBytes,
                pinnedBytes = state.pinnedBytes,
                budgetMb = state.budgetMb,
                downloading = state.downloading,
                downloadDone = state.downloadDone,
                downloadTotal = state.downloadTotal,
                formatBytes = { Formatter.formatShortFileSize(context, it) }
            )
        },
        settingsList = items,
        bottomContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SetupButton(
                    text = if (state.downloading) {
                        stringResource(Res.string.cloud_offline_downloading)
                    } else {
                        stringResource(Res.string.cloud_offline_download_now)
                    },
                    enabled = !state.downloading && state.accounts.any { it.pinned },
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    onClick = { actions.downloadNow() }
                )
                SetupButton(
                    text = stringResource(Res.string.cloud_offline_clear_data),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    onClick = { showClearDialog = true }
                )
            }
        }
    )

    if (showBudgetDialog) {
        BudgetDialog(
            currentMb = state.budgetMb,
            onDismiss = { showBudgetDialog = false },
            onSelect = {
                showBudgetDialog = false
                actions.setBudgetMb(it)
            }
        )
    }

    accountToUnpin?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToUnpin = null },
            title = {
                Text(stringResource(Res.string.cloud_offline_unpin_title, account.label))
            },
            text = {
                Text(stringResource(Res.string.cloud_offline_unpin_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    accountToUnpin = null
                    actions.unpinAccount(account.configId, true)
                }) {
                    Text(
                        stringResource(Res.string.cloud_offline_remove_downloads),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    accountToUnpin = null
                    actions.unpinAccount(account.configId, false)
                }) { Text(stringResource(Res.string.cloud_offline_keep_cached)) }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(Res.string.cloud_offline_clear_title)) },
            text = { Text(stringResource(Res.string.cloud_offline_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    actions.clearAllCache()
                }) {
                    Text(
                        stringResource(Res.string.cloud_offline_clear_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    actions.clearAutoCache()
                }) { Text(stringResource(Res.string.cloud_offline_auto_only)) }
            }
        )
    }

    accountSheet?.let { sheet ->
        AccountCacheSheet(
            state = sheet,
            formatBytes = { Formatter.formatShortFileSize(context, it) },
            onClearAccount = { actions.clearAccountCache(sheet.configId) },
            onClearAlbum = { remoteId -> actions.clearAlbumCache(sheet.configId, remoteId) },
            onDismiss = actions.closeAccountCache
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCacheSheet(
    state: AccountCacheSheetState,
    formatBytes: (Long) -> String,
    onClearAccount: () -> Unit,
    onClearAlbum: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = state.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    Res.string.cloud_offline_cached_size,
                    formatBytes(state.totalBytes),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            SetupButton(
                text = stringResource(Res.string.cloud_offline_clear_account),
                enabled = state.totalBytes > 0L,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                onClick = onClearAccount
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.cloud_offline_by_album),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            when {
                state.error != null -> {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                state.loading && state.albums.isEmpty() -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(Res.string.cloud_offline_loading_albums),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                state.albums.isEmpty() -> {
                    Text(
                        text = stringResource(Res.string.cloud_offline_no_albums),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.albums.forEach { album ->
                            AlbumCacheRow(
                                album = album,
                                formatBytes = formatBytes,
                                onClear = { onClearAlbum(album.remoteId) }
                            )
                        }
                        if (state.loading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCacheRow(
    album: AlbumCacheEntry,
    formatBytes: (Long) -> String,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatBytes(album.cacheBytes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        when {
            album.clearing -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )

            album.cacheBytes > 0L -> TextButton(onClick = onClear) {
                Text(
                    stringResource(Res.string.cloud_offline_clear),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            else -> Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StorageUsageCard(
    autoBytes: Long,
    pinnedBytes: Long,
    budgetMb: Int,
    downloading: Boolean,
    downloadDone: Int,
    downloadTotal: Int,
    formatBytes: (Long) -> String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = formatBytes(autoBytes + pinnedBytes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.cloud_offline_cached_content_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val budgetBytes = budgetMb.toLong() * 1024L * 1024L
            // Bar is scaled to the larger of the auto budget and what's actually
            // stored, so pinned media that exceeds the auto budget stays visible.
            val trackTotal = maxOf(budgetBytes, autoBytes + pinnedBytes, 1L)
            val freeBytes = (budgetBytes - autoBytes).coerceAtLeast(0L)
            val autoColor = MaterialTheme.colorScheme.primary
            val pinnedColor = MaterialTheme.colorScheme.tertiary
            val freeColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

            Spacer(Modifier.height(14.dp))
            SegmentedStorageBar(
                segments = listOf(autoColor to autoBytes, pinnedColor to pinnedBytes),
                total = trackTotal,
                trackColor = freeColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StorageLegendItem(
                    color = autoColor,
                    label = stringResource(Res.string.cloud_offline_auto),
                    value = formatBytes(autoBytes),
                )
                StorageLegendItem(
                    color = pinnedColor,
                    label = stringResource(Res.string.cloud_offline_pinned),
                    value = formatBytes(pinnedBytes),
                )
                StorageLegendItem(
                    color = freeColor,
                    label = stringResource(Res.string.cloud_offline_free),
                    value = formatBytes(freeBytes),
                )
            }
            if (downloading) {
                val p = if (downloadTotal > 0) downloadDone.toFloat() / downloadTotal else 0f
                LinearProgressIndicator(
                    progress = { p },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                Text(
                    stringResource(
                        Res.string.cloud_offline_download_progress,
                        downloadDone,
                        downloadTotal,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * A thick, rounded, multi-segment storage bar. Each segment is drawn proportional
 * to its byte count over [total]; the remainder shows as the faint [trackColor].
 */
@Composable
private fun SegmentedStorageBar(
    segments: List<Pair<Color, Long>>,
    total: Long,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(7.dp))) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)

        val safeTotal = total.coerceAtLeast(1L).toFloat()
        var x = 0f
        segments.forEach { (color, bytes) ->
            if (bytes <= 0L) return@forEach
            val w = size.width * (bytes.toFloat() / safeTotal)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(w, size.height),
                cornerRadius = radius
            )
            x += w
        }
    }
}

@Composable
private fun StorageLegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetDialog(currentMb: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val options = listOf(256, 512, 1024, 2048, 5120)
    val gigabyteUnit = stringResource(Res.string.gb)
    val optionLabels = options.associateWith { mb ->
        if (mb >= 1024) "${mb / 1024} $gigabyteUnit"
        else stringResource(Res.string.cloud_offline_megabytes, mb)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.cloud_offline_cache_limit)) },
        text = {
            Column {
                options.forEach { mb ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = mb == currentMb, onClick = { onSelect(mb) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mb == currentMb, onClick = { onSelect(mb) })
                        Text(
                            text = optionLabels.getValue(mb),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cloud_offline_done))
            }
        }
    )
}

private fun positionFor(index: Int, size: Int): Position = when {
    size == 1 -> Position.Alone
    index == 0 -> Position.Top
    index == size - 1 -> Position.Bottom
    else -> Position.Middle
}
