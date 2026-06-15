/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen

@Composable
fun UploadDetailsScreen() {
    val viewModel = hiltViewModel<UploadDetailsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val settingsList = remember(state) {
        buildList {
            val headerTitle = if (state.isWorkerRunning)
                context.getString(R.string.cloud_upload_uploading_count, state.completedItems, state.totalItems)
            else
                context.getString(R.string.cloud_upload_details_idle)
            add(SettingsEntity.Header(title = headerTitle))

            if (state.items.isEmpty() && !state.isWorkerRunning) {
                add(
                    SettingsEntity.Preference(
                        title = context.getString(R.string.cloud_upload_details_empty),
                        icon = Icons.Outlined.CloudUpload,
                        screenPosition = Position.Alone
                    )
                )
            } else {
                state.items.forEachIndexed { index, item ->
                    val statusText = when (item.status) {
                        UploadItemStatus.COMPLETED -> context.getString(R.string.cloud_upload_item_done)
                        UploadItemStatus.FAILED -> context.getString(R.string.cloud_upload_item_failed)
                        UploadItemStatus.UPLOADING -> context.getString(R.string.cloud_upload_item_uploading)
                        UploadItemStatus.PENDING -> context.getString(R.string.cloud_upload_item_pending)
                    }
                    val statusIcon = when (item.status) {
                        UploadItemStatus.COMPLETED -> Icons.Outlined.CheckCircle
                        UploadItemStatus.FAILED -> Icons.Outlined.Error
                        UploadItemStatus.UPLOADING -> Icons.Outlined.CloudUpload
                        UploadItemStatus.PENDING -> Icons.Outlined.HourglassTop
                    }
                    val pos = when {
                        state.items.size == 1 -> Position.Alone
                        index == 0 -> Position.Top
                        index == state.items.lastIndex -> Position.Bottom
                        else -> Position.Middle
                    }
                    add(
                        SettingsEntity.Preference(
                            title = item.fileName,
                            summary = statusText,
                            icon = statusIcon,
                            screenPosition = pos
                        )
                    )
                }
            }
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(R.string.cloud_upload_details),
        settingsList = settingsList,
        topContent = { UploadSummaryCard(state = state) }
    )
}

@Composable
private fun UploadSummaryCard(state: UploadDetailsUiState) {
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
                .background(
                    if (state.isWorkerRunning) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer
                )
                .animateContentSize()
                .padding(all = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isWorkerRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = if (state.isWorkerRunning) stringResource(R.string.cloud_upload_syncing)
                    else stringResource(R.string.cloud_upload_details_not_running),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.isWorkerRunning && state.totalItems > 0) {
                val progress = state.completedItems.toFloat() / state.totalItems
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.cloud_upload_completed_count, state.completedItems),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (state.failedItems > 0) {
                        Text(
                            text = stringResource(R.string.cloud_upload_failed_count, state.failedItems),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
