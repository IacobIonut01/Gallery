/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelStatus
import com.dot.gallery.feature_node.data.data_source.SmartScanRunEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanStatus
import com.dot.gallery.feature_node.presentation.settings.components.ChooserPreferenceDetailScreen
import com.dot.gallery.feature_node.presentation.settings.components.SettingsItem

@Composable
fun SmartScanPreferenceDetailScreen(
    viewModel: SmartFeaturesViewModel = hiltViewModel()
) {
    val searchStatus by viewModel.modelStatus(ModelGroup.SEARCH).collectAsStateWithLifecycle()
    val faceDetectStatus by viewModel.modelStatus(ModelGroup.FACE_DETECT).collectAsStateWithLifecycle()
    val faceRecognitionStatus by viewModel.modelStatus(ModelGroup.FACE_RECOGNITION).collectAsStateWithLifecycle()
    val activeScan by viewModel.activeSmartScan.collectAsStateWithLifecycle()
    val latestScan by viewModel.latestSmartScan.collectAsStateWithLifecycle()
    val searchReady = searchStatus == ModelStatus.READY
    val personsReady = faceDetectStatus == ModelStatus.READY && faceRecognitionStatus == ModelStatus.READY

    ChooserPreferenceDetailScreen<Unit>(
        title = stringResource(R.string.smart_scan_manager_title),
        description = stringResource(R.string.smart_scan_manager_description),
        customContent = {
            SmartScanDetailContent(
                activeScan = activeScan,
                latestScan = latestScan,
                searchReady = searchReady,
                personsReady = personsReady,
                onMetadata = viewModel::refreshMetadata,
                onEmbeddings = viewModel::refreshEmbeddings,
                onCategories = viewModel::refreshCategories,
                onPersons = viewModel::refreshPersons,
                onAll = viewModel::refreshAll,
                onFullRefresh = viewModel::fullRefresh,
                onCancel = viewModel::cancelActiveScan,
                onRetry = viewModel::retryLatestScan
            )
        }
    )
}

@Composable
private fun SmartScanDetailContent(
    activeScan: SmartScanRunEntity?,
    latestScan: SmartScanRunEntity?,
    searchReady: Boolean,
    personsReady: Boolean,
    onMetadata: () -> Unit,
    onEmbeddings: () -> Unit,
    onCategories: () -> Unit,
    onPersons: () -> Unit,
    onAll: () -> Unit,
    onFullRefresh: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val running = activeScan != null
    val retryable = latestScan?.status in setOf(
        SmartScanStatus.PARTIAL,
        SmartScanStatus.BLOCKED,
        SmartScanStatus.FAILED,
        SmartScanStatus.INTERRUPTED
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        activeScan?.let { ScanProgressCard(it) }
        if (activeScan == null && latestScan != null) ScanResultCard(latestScan)

        Text(
            text = stringResource(R.string.smart_scan_actions_header),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 8.dp)
        )
        ScanAction(
            title = stringResource(R.string.refresh_metadata),
            summary = stringResource(R.string.smart_scan_metadata_detail),
            position = Position.Top,
            enabled = !running,
            onClick = onMetadata
        )
        ScanAction(
            title = stringResource(R.string.refresh_embeddings),
            summary = if (searchReady) stringResource(R.string.refresh_embeddings_summary)
            else stringResource(R.string.ai_models_unavailable),
            position = Position.Middle,
            enabled = !running && searchReady,
            onClick = onEmbeddings
        )
        ScanAction(
            title = stringResource(R.string.refresh_categories),
            summary = if (searchReady) stringResource(R.string.refresh_categories_summary)
            else stringResource(R.string.ai_models_unavailable),
            position = Position.Middle,
            enabled = !running && searchReady,
            onClick = onCategories
        )
        ScanAction(
            title = stringResource(R.string.refresh_persons),
            summary = if (personsReady) stringResource(R.string.refresh_persons_summary)
            else stringResource(R.string.smart_scan_face_models_unavailable),
            position = Position.Middle,
            enabled = !running && personsReady,
            onClick = onPersons
        )
        ScanAction(
            title = stringResource(R.string.refresh_all_smart_features),
            summary = stringResource(R.string.refresh_all_smart_features_summary),
            position = Position.Bottom,
            enabled = !running,
            onClick = onAll
        )

        Text(
            text = stringResource(R.string.smart_scan_rebuild_header),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp)
        )
        ScanAction(
            title = stringResource(R.string.smart_scan_full_refresh),
            summary = stringResource(R.string.smart_scan_full_refresh_summary),
            position = Position.Alone,
            enabled = !running,
            onClick = onFullRefresh
        )

        AnimatedVisibility(visible = running || retryable) {
            Column {
                Text(
                    text = stringResource(R.string.smart_scan_controls_header),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp)
                )
                if (running) {
                    ScanAction(
                        title = stringResource(R.string.smart_scan_cancel),
                        summary = stringResource(R.string.smart_scan_cancel_summary),
                        position = Position.Alone,
                        enabled = true,
                        onClick = onCancel
                    )
                } else if (retryable) {
                    ScanAction(
                        title = stringResource(R.string.smart_scan_retry),
                        summary = stringResource(R.string.smart_scan_retry_summary),
                        position = Position.Alone,
                        enabled = true,
                        onClick = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanProgressCard(run: SmartScanRunEntity) {
    val progress = if (run.totalMedia <= 0) 0f
    else (run.processedMedia.toFloat() / run.totalMedia.toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.smart_scan_running_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(
                R.string.smart_scan_progress,
                run.currentPhase?.storedValue.orEmpty(),
                (progress * 100).toInt(),
                run.failedMedia,
                run.skippedMedia
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScanResultCard(run: SmartScanRunEntity) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.smart_scan_latest_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(
                R.string.smart_scan_last_result,
                run.status.storedValue,
                run.processedMedia,
                run.failedMedia,
                run.skippedMedia
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ScanAction(
    title: String,
    summary: String,
    position: Position,
    enabled: Boolean,
    onClick: () -> Unit
) {
    SettingsItem(
        item = SettingsEntity.Preference(
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = onClick,
            screenPosition = position
        )
    )
}
