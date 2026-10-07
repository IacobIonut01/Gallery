/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.cast.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.app.presentation.cast.CastSessionState
import com.dot.gallery.app.presentation.cast.FCastDevice
import com.dot.gallery.app.presentation.cast.FCastProtocol
import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.domain.uri.host
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cancel
import com.dot.gallery.generated.resources.cast_connect_button
import com.dot.gallery.generated.resources.cast_connected_to
import com.dot.gallery.generated.resources.cast_disconnect
import com.dot.gallery.generated.resources.cast_enter_ip
import com.dot.gallery.generated.resources.cast_ip_hint
import com.dot.gallery.generated.resources.cast_manual_connect
import com.dot.gallery.generated.resources.cast_port_hint
import com.dot.gallery.generated.resources.cast_searching
import com.dot.gallery.generated.resources.cast_select_device
import com.dot.gallery.generated.resources.cast_stop_current
import com.dot.gallery.generated.resources.cast_this_media
import com.dot.gallery.generated.resources.close


@Composable
fun FCastDevicePickerDialog(
    state: CastSessionState,
    onDeviceSelected: (FCastDevice) -> Unit,
    onCastMedia: () -> Unit = {},
    onStopCasting: () -> Unit = {},
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (state.connectedDevice != null) {
                    Icons.Outlined.CastConnected
                } else {
                    Icons.Outlined.Cast
                },
                contentDescription = null
            )
        },
        title = {
            Text(
                text = if (state.connectedDevice != null) {
                    stringResource(Res.string.cast_connected_to, state.connectedDevice.name)
                } else {
                    stringResource(Res.string.cast_select_device)
                }
            )
        },
        text = {
            Column {
                if (state.connectedDevice != null) {
                    // "Cast this media" action
                    ActionItem(
                        icon = Icons.Outlined.PlayCircleOutline,
                        text = stringResource(Res.string.cast_this_media),
                        onClick = {
                            onCastMedia()
                            onDismiss()
                        }
                    )

                    // "Stop current cast" action (only when actively casting)
                    if (state.castingMediaId != null) {
                        ActionItem(
                            icon = Icons.Outlined.StopCircle,
                            text = stringResource(Res.string.cast_stop_current),
                            onClick = {
                                onStopCasting()
                                onDismiss()
                            }
                        )
                    }

                    // "Disconnect" action
                    ActionItem(
                        icon = Icons.Outlined.LinkOff,
                        text = stringResource(Res.string.cast_disconnect),
                        onClick = onDisconnect,
                        tint = MaterialTheme.colorScheme.error
                    )
                } else {
                    if (state.isDiscovering && state.discoveredDevices.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(Res.string.cast_searching),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (state.discoveredDevices.isNotEmpty()) {
                        LazyColumn {
                            items(state.discoveredDevices) { device ->
                                DeviceItem(
                                    device = device,
                                    isConnecting = state.isConnecting,
                                    onClick = { onDeviceSelected(device) }
                                )
                            }
                        }
                    }

                    if (state.connectionError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.connectionError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Manual connection
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.cast_manual_connect),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    var manualIp by rememberSaveable { mutableStateOf("") }
                    var manualPort by rememberSaveable { mutableStateOf("") }
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it.trim() },
                        label = { Text(stringResource(Res.string.cast_enter_ip)) },
                        placeholder = { Text(stringResource(Res.string.cast_ip_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(Res.string.cast_port_hint)) },
                            placeholder = { Text(FCastProtocol.DEFAULT_PORT.toString()) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                if (manualIp.isNotBlank()) {
                                    val port = manualPort.toIntOrNull()
                                        ?: FCastProtocol.DEFAULT_PORT
                                    onDeviceSelected(
                                        FCastDevice(
                                            name = manualIp,
                                            host = manualIp,
                                            port = port
                                        )
                                    )
                                }
                            },
                            enabled = manualIp.isNotBlank() && !state.isConnecting
                        ) {
                            Text(stringResource(Res.string.cast_connect_button))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(
                        if (state.connectedDevice != null) Res.string.close
                        else Res.string.cancel
                    )
                )
            }
        }
    )
}

@Composable
private fun DeviceItem(
    device: FCastDevice,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Tv,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${device.host}:${device.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isConnecting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = tint
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
