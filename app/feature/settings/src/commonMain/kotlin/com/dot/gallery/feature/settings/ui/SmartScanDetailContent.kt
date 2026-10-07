/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.smart.SmartScanPlan
import com.dot.gallery.core.ui.components.ChooserPreferenceDetailScreen
import com.dot.gallery.core.ui.components.SettingsItem
import com.dot.gallery.data.entity.SmartScanPhaseEntity
import com.dot.gallery.data.entity.SmartScanRunEntity
import com.dot.gallery.data.entity.SmartScanStatus
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.settings.generated.resources.Res
import com.dot.gallery.feature.settings.generated.resources.ai_models_unavailable
import com.dot.gallery.feature.settings.generated.resources.refresh_all_smart_features
import com.dot.gallery.feature.settings.generated.resources.refresh_all_smart_features_summary
import com.dot.gallery.feature.settings.generated.resources.refresh_categories
import com.dot.gallery.feature.settings.generated.resources.refresh_categories_summary
import com.dot.gallery.feature.settings.generated.resources.refresh_embeddings
import com.dot.gallery.feature.settings.generated.resources.refresh_embeddings_summary
import com.dot.gallery.feature.settings.generated.resources.refresh_metadata
import com.dot.gallery.feature.settings.generated.resources.refresh_persons
import com.dot.gallery.feature.settings.generated.resources.refresh_persons_summary
import com.dot.gallery.feature.settings.generated.resources.smart_scan_actions_header
import com.dot.gallery.feature.settings.generated.resources.smart_scan_cancel
import com.dot.gallery.feature.settings.generated.resources.smart_scan_cancel_summary
import com.dot.gallery.feature.settings.generated.resources.smart_scan_controls_header
import com.dot.gallery.feature.settings.generated.resources.smart_scan_face_models_unavailable
import com.dot.gallery.feature.settings.generated.resources.smart_scan_full_refresh
import com.dot.gallery.feature.settings.generated.resources.smart_scan_full_refresh_summary
import com.dot.gallery.feature.settings.generated.resources.smart_scan_last_result
import com.dot.gallery.feature.settings.generated.resources.smart_scan_latest_title
import com.dot.gallery.feature.settings.generated.resources.smart_scan_manager_description
import com.dot.gallery.feature.settings.generated.resources.smart_scan_manager_title
import com.dot.gallery.feature.settings.generated.resources.smart_scan_metadata_detail
import com.dot.gallery.feature.settings.generated.resources.smart_scan_preparing_stage
import com.dot.gallery.feature.settings.generated.resources.smart_scan_rebuild_header
import com.dot.gallery.feature.settings.generated.resources.smart_scan_retry
import com.dot.gallery.feature.settings.generated.resources.smart_scan_retry_summary
import com.dot.gallery.feature.settings.generated.resources.smart_scan_running_title
import com.dot.gallery.feature.settings.generated.resources.smart_scan_stage_items
import com.dot.gallery.feature.settings.generated.resources.smart_scan_stage_number

@Composable
fun SmartScanDetailContent(
    state: SmartScanDetailState,
    actions: SmartScanDetailActions = SmartScanDetailActions(),
) {
    ChooserPreferenceDetailScreen<Unit>(
        title = stringResource(Res.string.smart_scan_manager_title),
        description = stringResource(Res.string.smart_scan_manager_description),
        customContent = {
            SmartScanDetailBody(state = state, actions = actions)
        }
    )
}

@Composable
private fun SmartScanDetailBody(
    state: SmartScanDetailState,
    actions: SmartScanDetailActions,
) {
    val running = state.activeScan != null
    val retryable = state.latestScan?.status in setOf(
        SmartScanStatus.PARTIAL,
        SmartScanStatus.BLOCKED,
        SmartScanStatus.FAILED,
        SmartScanStatus.INTERRUPTED
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        state.activeScan?.let { ScanProgressCard(it, state.activePhases) }
        if (state.activeScan == null && state.latestScan != null) ScanResultCard(state.latestScan)

        SettingsItem(
            item = SettingsEntity.Header(
                title = stringResource(Res.string.smart_scan_actions_header)
            )
        )
        ScanAction(
            title = stringResource(Res.string.refresh_metadata),
            summary = stringResource(Res.string.smart_scan_metadata_detail),
            position = Position.Top,
            enabled = !running,
            onClick = actions.onMetadata
        )
        ScanAction(
            title = stringResource(Res.string.refresh_embeddings),
            summary = if (state.searchReady) stringResource(Res.string.refresh_embeddings_summary)
            else stringResource(Res.string.ai_models_unavailable),
            position = Position.Middle,
            enabled = !running && state.searchReady,
            onClick = actions.onEmbeddings
        )
        ScanAction(
            title = stringResource(Res.string.refresh_categories),
            summary = if (state.searchReady) stringResource(Res.string.refresh_categories_summary)
            else stringResource(Res.string.ai_models_unavailable),
            position = Position.Middle,
            enabled = !running && state.searchReady,
            onClick = actions.onCategories
        )
        ScanAction(
            title = stringResource(Res.string.refresh_persons),
            summary = if (state.personsReady) stringResource(Res.string.refresh_persons_summary)
            else stringResource(Res.string.smart_scan_face_models_unavailable),
            position = Position.Middle,
            enabled = !running && state.personsReady,
            onClick = actions.onPersons
        )
        ScanAction(
            title = stringResource(Res.string.refresh_all_smart_features),
            summary = stringResource(Res.string.refresh_all_smart_features_summary),
            position = Position.Bottom,
            enabled = !running,
            onClick = actions.onAll
        )

        SettingsItem(
            item = SettingsEntity.Header(
                title = stringResource(Res.string.smart_scan_rebuild_header)
            )
        )
        ScanAction(
            title = stringResource(Res.string.smart_scan_full_refresh),
            summary = stringResource(Res.string.smart_scan_full_refresh_summary),
            position = Position.Alone,
            enabled = !running,
            onClick = actions.onFullRefresh
        )

        AnimatedVisibility(visible = running || retryable) {
            Column {
                SettingsItem(
                    item = SettingsEntity.Header(
                        title = stringResource(Res.string.smart_scan_controls_header)
                    )
                )
                if (running) {
                    ScanAction(
                        title = stringResource(Res.string.smart_scan_cancel),
                        summary = stringResource(Res.string.smart_scan_cancel_summary),
                        position = Position.Alone,
                        enabled = true,
                        onClick = actions.onCancel
                    )
                } else if (retryable) {
                    ScanAction(
                        title = stringResource(Res.string.smart_scan_retry),
                        summary = stringResource(Res.string.smart_scan_retry_summary),
                        position = Position.Alone,
                        enabled = true,
                        onClick = actions.onRetry
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
                        text = stringResource(Res.string.smart_scan_running_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            Res.string.smart_scan_stage_number,
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
                                Res.string.smart_scan_stage_items,
                                phase.processedMedia,
                                phase.totalMedia,
                                phase.failedMedia,
                                phase.skippedMedia
                            )
                        } else {
                            stringResource(Res.string.smart_scan_preparing_stage)
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
            text = stringResource(Res.string.smart_scan_latest_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(
                Res.string.smart_scan_last_result,
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
