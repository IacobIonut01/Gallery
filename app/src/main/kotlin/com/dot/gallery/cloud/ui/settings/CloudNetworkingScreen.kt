/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.cloud.ui.WifiSsidPickerSheet
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private fun parseExternalUrls(json: String): List<String> {
    return try {
        Json.decodeFromString<List<String>>(json)
    } catch (_: Exception) { emptyList() }
}

private fun serializeExternalUrls(urls: List<String>): String {
    return Json.encodeToString(urls)
}

@Composable
fun CloudNetworkingScreen(configId: Long) {
    val settingsVm = hiltViewModel<CloudSettingsViewModel>()
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()

    LaunchedEffect(configId) { settingsVm.loadConfig(configId) }
    if (config == null) {
        AccountSettingsStateScreen(stringResource(R.string.cloud_networking), loadState)
        return
    }
    val autoUrlSwitch = config?.autoUrlSwitch ?: false
    val serverUrl = config?.serverUrl ?: ""
    val wifiSsid = config?.localWifiSsid ?: ""
    val localUrl = config?.localServerUrl ?: ""
    val externalUrls = remember(config?.externalUrls) {
        parseExternalUrls(config?.externalUrls ?: "[]")
    }
    val effectiveUrl = remember(config) { settingsVm.effectiveUrl() }
    val localActive = remember(config) { settingsVm.isLocalActive() }

    val scope = rememberCoroutineScope()
    val wifiSheetState = rememberAppBottomSheetState()

    var showLocalUrlDialog by remember { mutableStateOf(false) }
    var showAddExternalDialog by remember { mutableStateOf(false) }
    val localHeader = stringResource(R.string.cloud_net_local)
    val wifiNameTitle = stringResource(R.string.cloud_net_wifi_name)
    val wifiBlankSummary = stringResource(R.string.cloud_net_wifi_blank_summary)
    val localUrlTitle = stringResource(R.string.cloud_net_local_url)
    val notConfiguredSummary = stringResource(R.string.cloud_net_not_configured)
    val externalHeader = stringResource(R.string.cloud_net_external)
    val addExternalTitle = stringResource(R.string.cloud_net_add_external)
    val noExternalSummary = stringResource(R.string.cloud_net_no_external)
    val resourceStrings = listOf(
        localHeader,
        wifiNameTitle,
        wifiBlankSummary,
        localUrlTitle,
        notConfiguredSummary,
        externalHeader,
        addExternalTitle,
        noExternalSummary,
    )

    val settingsList = remember(
        autoUrlSwitch,
        wifiSsid,
        localUrl,
        externalUrls,
        resourceStrings,
    ) {
        buildList {
            if (autoUrlSwitch) {
                // Local network
                add(SettingsEntity.Header(title = localHeader))
                add(
                    SettingsEntity.Preference(
                        title = wifiNameTitle,
                        summary = wifiSsid.ifBlank { wifiBlankSummary },
                        onClick = { scope.launch { wifiSheetState.show() } },
                        screenPosition = Position.Top
                    )
                )
                add(
                    SettingsEntity.Preference(
                        title = localUrlTitle,
                        summary = localUrl.ifBlank { notConfiguredSummary },
                        onClick = { showLocalUrlDialog = true },
                        screenPosition = Position.Bottom
                    )
                )

                // External URLs
                add(SettingsEntity.Header(title = externalHeader))
                if (externalUrls.isEmpty()) {
                    add(
                        SettingsEntity.Preference(
                            title = addExternalTitle,
                            summary = noExternalSummary,
                            onClick = { showAddExternalDialog = true },
                            screenPosition = Position.Alone
                        )
                    )
                } else {
                    externalUrls.forEachIndexed { index, url ->
                        val pos = when {
                            externalUrls.size == 1 -> Position.Alone
                            index == 0 -> Position.Top
                            index == externalUrls.lastIndex -> Position.Bottom
                            else -> Position.Middle
                        }
                        add(
                            SettingsEntity.Preference(
                                title = url,
                                onSwipeToDelete = {
                                    val updated = externalUrls.filter { it != url }
                                    settingsVm.updateConfig { copy(externalUrls = serializeExternalUrls(updated)) }
                                },
                                screenPosition = pos
                            )
                        )
                    }
                    add(
                        SettingsEntity.Preference(
                            title = addExternalTitle,
                            onClick = { showAddExternalDialog = true },
                            screenPosition = Position.Alone
                        )
                    )
                }
            }
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(R.string.cloud_networking),
        settingsList = settingsList,
        topContent = {
            NetworkingHeroCard(
                effectiveUrl = effectiveUrl?.ifBlank { null } ?: serverUrl,
                localActive = localActive,
                autoUrlSwitch = autoUrlSwitch,
                onToggleAuto = { settingsVm.updateConfig { copy(autoUrlSwitch = it) } }
            )
        }
    )

    // WiFi SSID picker sheet (pick connected network or manual entry; blank = disable switching)
    WifiSsidPickerSheet(
        state = wifiSheetState,
        currentSsid = wifiSsid,
        onSave = { ssid -> settingsVm.updateConfig { copy(localWifiSsid = ssid) } }
    )

    // Local URL edit dialog
    if (showLocalUrlDialog) {
        var text by remember { mutableStateOf(localUrl) }
        AlertDialog(
            onDismissRequest = { showLocalUrlDialog = false },
            title = { Text(stringResource(R.string.cloud_net_edit_local_url)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.cloud_net_external_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    settingsVm.updateConfig { copy(localServerUrl = text.trim()) }
                    showLocalUrlDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showLocalUrlDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Add external URL dialog
    if (showAddExternalDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddExternalDialog = false },
            title = { Text(stringResource(R.string.cloud_net_add_external)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.cloud_net_external_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) {
                        val current = parseExternalUrls(config?.externalUrls ?: "[]")
                        val updated = current + text.trim()
                        settingsVm.updateConfig { copy(externalUrls = serializeExternalUrls(updated)) }
                    }
                    showAddExternalDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddExternalDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/**
 * Fancy connection card shown above the networking settings DSL. It visualises which
 * URL (local vs external) is currently in effect with two connected mode tiles, an
 * animated "active" glow, the resolved server address, and the automatic-switching
 * toggle — so the local/external state is obvious at a glance.
 */
@Composable
private fun NetworkingHeroCard(
    effectiveUrl: String,
    localActive: Boolean,
    autoUrlSwitch: Boolean,
    onToggleAuto: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Active address headline
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (localActive) Icons.Outlined.Wifi else Icons.Outlined.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cloud_net_connection),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = effectiveUrl.ifBlank { stringResource(R.string.cloud_net_not_configured) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Local <-> External mode selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NetworkModeTile(
                icon = Icons.Outlined.Router,
                label = stringResource(R.string.cloud_net_local),
                active = localActive,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            NetworkModeTile(
                icon = Icons.Outlined.Public,
                label = stringResource(R.string.cloud_net_external),
                active = !localActive,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = stringResource(
                if (localActive) R.string.cloud_net_local_active
                else R.string.cloud_net_external_active
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Automatic switching toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cloud_net_auto_url),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.cloud_net_auto_url_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = autoUrlSwitch, onCheckedChange = onToggleAuto)
        }
    }
}

@Composable
private fun NetworkModeTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(300),
        label = "tileContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "tileContent"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
            if (active) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
