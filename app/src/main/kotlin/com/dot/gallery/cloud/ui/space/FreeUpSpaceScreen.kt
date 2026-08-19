/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.space

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
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

    // "Never" (sentinel -1) is listed first and is the default so nothing is ever
    // removed until the user explicitly opts into an age range.
    val cutoffOptions = listOf(FreeUpSpaceViewModel.NEVER_CUTOFF, 30, 60, 90, 180, 365)
    val keepFavoritesTitle = stringResource(R.string.cloud_free_space_keep_favorites)
    val keepFavoritesSummary = stringResource(R.string.cloud_free_space_keep_favorites_summary)
    val cutoffHeader = stringResource(R.string.cloud_free_space_cutoff)
    val cutoffLabels = mapOf(
        FreeUpSpaceViewModel.NEVER_CUTOFF to stringResource(R.string.cloud_free_space_never),
        30 to stringResource(R.string.cloud_free_space_30d),
        60 to stringResource(R.string.cloud_free_space_60d),
        90 to stringResource(R.string.cloud_free_space_90d),
        180 to stringResource(R.string.cloud_free_space_6m),
        365 to stringResource(R.string.cloud_free_space_1y),
    )
    val scanningTitle = stringResource(R.string.cloud_free_space_scanning)
    val scanTitle = stringResource(R.string.cloud_free_space_scan)
    val resourceStrings = listOf(
        keepFavoritesTitle,
        keepFavoritesSummary,
        cutoffHeader,
        scanningTitle,
        scanTitle,
    ) + cutoffLabels.values

    val settingsList = remember(
        state.keepFavorites,
        state.cutoffDays,
        state.isScanning,
        state.isDeleting,
        resourceStrings,
    ) {
        buildList {
            // Options
            add(SettingsEntity.Header(title = ""))
            add(
                SettingsEntity.SwitchPreference(
                    title = keepFavoritesTitle,
                    summary = keepFavoritesSummary,
                    isChecked = state.keepFavorites,
                    onCheck = { viewModel.setKeepFavorites(it) },
                    screenPosition = Position.Alone
                )
            )

            // Cutoff period
            add(SettingsEntity.Header(title = cutoffHeader))
            cutoffOptions.forEachIndexed { index, days ->
                val pos = when {
                    cutoffOptions.size == 1 -> Position.Alone
                    index == 0 -> Position.Top
                    index == cutoffOptions.lastIndex -> Position.Bottom
                    else -> Position.Middle
                }
                add(
                    SettingsEntity.Preference(
                        title = cutoffLabels.getValue(days),
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
                    title = if (state.isScanning) scanningTitle else scanTitle,
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
                    Text(
                        text = stringResource(R.string.cloud_local_deletion_unavailable),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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
