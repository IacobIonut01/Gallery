/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.dot.gallery.core.smart.SmartScanPlan
import com.dot.gallery.feature_node.data.data_source.SmartScanPhase
import com.dot.gallery.feature_node.data.data_source.SmartScanPhaseEntity
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
    val activePhases by viewModel.activeSmartScanPhases.collectAsStateWithLifecycle()
    val latestScan by viewModel.latestSmartScan.collectAsStateWithLifecycle()
    val searchReady = searchStatus == ModelStatus.READY
    val personsReady = faceDetectStatus == ModelStatus.READY && faceRecognitionStatus == ModelStatus.READY

    ChooserPreferenceDetailScreen<Unit>(
        title = stringResource(R.string.smart_scan_manager_title),
        description = stringResource(R.string.smart_scan_manager_description),
        customContent = {
            SmartScanDetailContent(
                activeScan = activeScan,
                activePhases = activePhases,
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
    activePhases: List<SmartScanPhaseEntity>,
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
        activeScan?.let { ScanProgressCard(it, activePhases) }
        if (activeScan == null && latestScan != null) ScanResultCard(latestScan)

        SettingsItem(
            item = SettingsEntity.Header(
                title = stringResource(R.string.smart_scan_actions_header)
            )
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

        SettingsItem(
            item = SettingsEntity.Header(
                title = stringResource(R.string.smart_scan_rebuild_header)
            )
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
                SettingsItem(
                    item = SettingsEntity.Header(
                        title = stringResource(R.string.smart_scan_controls_header)
                    )
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
private fun ScanProgressCard(run: SmartScanRunEntity, phases: List<SmartScanPhaseEntity>) {
    val current = phases.firstOrNull { it.status == SmartScanStatus.RUNNING }
        ?: phases.firstOrNull { it.status == SmartScanStatus.QUEUED }
    val overallProgress = SmartScanPlan.overallProgress(phases)
    val stageNumber = phases.indexOf(current).takeIf { it >= 0 }?.plus(1) ?: 0
    val estimatedRemainingMillis = current
        ?.takeIf { it.status == SmartScanStatus.RUNNING }
        ?.let { SmartScanPlan.estimatedRemainingMillis(it.totalMedia, it.processedMedia, it.startedAt) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.smart_scan_running_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.smart_scan_stage_number,
                            stageNumber,
                            phases.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier.fillMaxWidth()
            )
            current?.let { phase ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = phase.phase.label(),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (phase.totalMedia > 0) {
                            stringResource(
                                R.string.smart_scan_stage_items,
                                phase.processedMedia,
                                phase.totalMedia,
                                phase.failedMedia,
                                phase.skippedMedia
                            )
                        } else {
                            stringResource(R.string.smart_scan_preparing_stage)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    estimatedRemainingMillis?.let {
                        Text(
                            text = smartScanEtaLabel(it),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (phase.totalMedia > 0) {
                        LinearProgressIndicator(
                            progress = {
                                (phase.processedMedia.toFloat() / phase.totalMedia).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                phases.forEach { phase ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = phase.phase.label(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = phase.status.label(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (phase.status == SmartScanStatus.RUNNING) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
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
                run.status.label(),
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
internal fun smartScanEtaLabel(estimatedRemainingMillis: Long): String {
    val totalMinutes = (estimatedRemainingMillis / 60_000L).coerceAtLeast(0L)
    if (totalMinutes < 1) return stringResource(R.string.smart_scan_eta_under_minute)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.smart_scan_eta_hours, hours, minutes)
    } else {
        stringResource(R.string.smart_scan_eta_minutes, totalMinutes)
    }
}

@Composable
internal fun SmartScanPhase.label(): String = stringResource(
    when (this) {
        SmartScanPhase.SOURCE_SYNC -> R.string.smart_scan_phase_source_sync
        SmartScanPhase.METADATA -> R.string.smart_scan_phase_metadata
        SmartScanPhase.SEARCH_INDEX -> R.string.smart_scan_phase_search_index
        SmartScanPhase.CATEGORY_CLASSIFICATION -> R.string.smart_scan_phase_categories
        SmartScanPhase.FACE_INDEX -> R.string.smart_scan_phase_people
    }
)

@Composable
internal fun SmartScanStatus.label(): String = stringResource(
    when (this) {
        SmartScanStatus.QUEUED -> R.string.smart_scan_status_queued
        SmartScanStatus.RUNNING -> R.string.smart_scan_status_running
        SmartScanStatus.SUCCEEDED -> R.string.smart_scan_status_complete
        SmartScanStatus.PARTIAL -> R.string.smart_scan_status_partial
        SmartScanStatus.BLOCKED -> R.string.smart_scan_status_blocked
        SmartScanStatus.INTERRUPTED -> R.string.smart_scan_status_interrupted
        SmartScanStatus.FAILED -> R.string.smart_scan_status_failed
        SmartScanStatus.CANCELLED -> R.string.smart_scan_status_cancelled
    }
)

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
