/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.space

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen

@Composable
fun FreeUpSpaceScreen() {
    val viewModel = hiltViewModel<FreeUpSpaceViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val cutoffOptions = listOf(30, 60, 90, 180, 365)

    val settingsList = remember(state.keepFavorites, state.cutoffDays) {
        buildList {
            // Options
            add(SettingsEntity.Header(title = ""))
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_free_space_keep_favorites),
                    summary = context.getString(R.string.cloud_free_space_keep_favorites_summary),
                    isChecked = state.keepFavorites,
                    onCheck = { viewModel.setKeepFavorites(it) },
                    screenPosition = Position.Alone
                )
            )

            // Cutoff period
            add(SettingsEntity.Header(title = context.getString(R.string.cloud_free_space_cutoff)))
            cutoffOptions.forEachIndexed { index, days ->
                val label = when (days) {
                    30 -> context.getString(R.string.cloud_free_space_30d)
                    60 -> context.getString(R.string.cloud_free_space_60d)
                    90 -> context.getString(R.string.cloud_free_space_90d)
                    180 -> context.getString(R.string.cloud_free_space_6m)
                    365 -> context.getString(R.string.cloud_free_space_1y)
                    else -> "$days days"
                }
                val pos = when {
                    cutoffOptions.size == 1 -> Position.Alone
                    index == 0 -> Position.Top
                    index == cutoffOptions.lastIndex -> Position.Bottom
                    else -> Position.Middle
                }
                add(
                    SettingsEntity.Preference(
                        title = label,
                        rightText = if (state.cutoffDays == days) "✓" else null,
                        onClick = { viewModel.setCutoffDays(days) },
                        screenPosition = pos
                    )
                )
            }

            // Scan action
            add(SettingsEntity.Header(title = ""))
            add(
                SettingsEntity.Preference(
                    title = if (state.isScanning) context.getString(R.string.cloud_free_space_scanning)
                    else context.getString(R.string.cloud_free_space_scan),
                    enabled = !state.isScanning && !state.isDeleting,
                    onClick = { viewModel.scan() },
                    screenPosition = Position.Alone
                )
            )
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(R.string.cloud_free_space),
        settingsList = settingsList,
        topContent = {
            Text(
                text = stringResource(R.string.cloud_free_space_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        },
        bottomContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                if (state.message.isNotEmpty()) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (state.backedUpItems.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.deleteLocalCopies() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        } else {
                            Icon(Icons.Outlined.CleaningServices, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(stringResource(R.string.cloud_free_space_remove_count, state.backedUpItems.size))
                    }
                }
                state.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    )
}
