/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Scanner
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.ui.components.SettingsItem
import com.dot.gallery.core.ui.components.SwitchPreferenceDetailScreen
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.data.entity.SmartScanStatus
import com.dot.gallery.domain.settings.SettingsKeys
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.settings.generated.resources.Res
import com.dot.gallery.feature.settings.generated.resources.action_cancel
import com.dot.gallery.feature.settings.generated.resources.ai_models_not_available
import com.dot.gallery.feature.settings.generated.resources.categories_exclude_videos
import com.dot.gallery.feature.settings.generated.resources.categories_exclude_videos_purge_message
import com.dot.gallery.feature.settings.generated.resources.categories_exclude_videos_purge_title
import com.dot.gallery.feature.settings.generated.resources.categories_exclude_videos_summary
import com.dot.gallery.feature.settings.generated.resources.categories_settings
import com.dot.gallery.feature.settings.generated.resources.categories_stop_scan
import com.dot.gallery.feature.settings.generated.resources.categories_stop_scan_summary
import com.dot.gallery.feature.settings.generated.resources.categorise_your_media
import com.dot.gallery.feature.settings.generated.resources.disclaimer_classification
import com.dot.gallery.feature.settings.generated.resources.editor_remove
import com.dot.gallery.feature.settings.generated.resources.reset_categories
import com.dot.gallery.feature.settings.generated.resources.reset_categories_summary
import com.dot.gallery.feature.settings.generated.resources.scan_for_new_categories
import com.dot.gallery.feature.settings.generated.resources.scanning_media

@Composable
fun CategoriesSettingsContent(
    state: CategoriesSettingsState,
    actions: CategoriesSettingsActions = CategoriesSettingsActions(),
) {
    var noClassification by rememberPreference(SettingsKeys.noClassification)
    var excludeVideos by rememberPreference(SettingsKeys.categoriesExcludeVideos)

    val isCategoryWorkerRunning = state.categoryScan != null

    SwitchPreferenceDetailScreen(
        title = stringResource(Res.string.categories_settings),
        isChecked = !noClassification,
        onCheckedChange = { noClassification = !it },
        switchLabel = stringResource(Res.string.categorise_your_media),
        description = stringResource(Res.string.disclaimer_classification),
        customContent = {
            Column {
                // Scanner button
                SettingsItem(
                    item = SettingsEntity.Preference(
                        title = if (isCategoryWorkerRunning)
                            stringResource(Res.string.scanning_media)
                        else
                            stringResource(Res.string.scan_for_new_categories),
                        summary = if (!state.modelsReady)
                            stringResource(Res.string.ai_models_not_available)
                        else null,
                        icon = Icons.Outlined.Scanner,
                        screenPosition = if (state.hasCategories || isCategoryWorkerRunning)
                            Position.Top else Position.Alone
                    ),
                    modifier = Modifier
                        .alpha(if (state.modelsReady) 1f else 0.5f)
                        .combinedClickable(
                            enabled = state.modelsReady,
                            onLongClick = {
                                if (isCategoryWorkerRunning) actions.onStopScan()
                            },
                            onClick = {
                                if (!isCategoryWorkerRunning) actions.onStartScan()
                            }
                        )
                )

                // Exclude videos from categorisation (#948)
                SettingsItem(
                    item = SettingsEntity.SwitchPreference(
                        title = stringResource(Res.string.categories_exclude_videos),
                        summary = stringResource(Res.string.categories_exclude_videos_summary),
                        icon = Icons.Outlined.VideocamOff,
                        isChecked = excludeVideos,
                        onCheck = { checked ->
                            if (!checked) {
                                excludeVideos = false
                            } else {
                                actions.onRequestExcludeVideos()
                            }
                        },
                        screenPosition = if (state.hasCategories || isCategoryWorkerRunning)
                            Position.Middle else Position.Alone
                    )
                )

                // Progress indicator when scanning
                AnimatedVisibility(visible = isCategoryWorkerRunning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateContentSize()
                    ) {
                        if (state.workerProgress < 100f) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = { (state.workerProgress / 100f).coerceAtLeast(0f) },
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        state.categoryScan?.let { run ->
                            Text(
                                text = when (run.status) {
                                    SmartScanStatus.RUNNING ->
                                        run.currentPhase?.label() ?: run.status.label()
                                    else -> run.status.label()
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }

                // Stop action while a scan is visible
                if (isCategoryWorkerRunning) {
                    SettingsItem(
                        item = SettingsEntity.Preference(
                            title = stringResource(Res.string.categories_stop_scan),
                            summary = stringResource(Res.string.categories_stop_scan_summary),
                            onClick = actions.onStopScan,
                            screenPosition = Position.Bottom
                        )
                    )
                }

                // Reset categories
                if (state.hasCategories && !isCategoryWorkerRunning) {
                    SettingsItem(
                        item = SettingsEntity.Preference(
                            title = stringResource(Res.string.reset_categories),
                            summary = stringResource(Res.string.reset_categories_summary),
                            icon = Icons.Default.Refresh,
                            onClick = actions.onResetCategories,
                            screenPosition = Position.Bottom
                        ),
                        backgroundColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
            }
        }
    )

    state.videoPurgeCount?.let { count ->
        AlertDialog(
            onDismissRequest = actions.onDismissVideoPurge,
            title = { Text(stringResource(Res.string.categories_exclude_videos_purge_title)) },
            text = {
                Text(pluralStringResource(Res.plurals.categories_exclude_videos_purge_message, count, count))
            },
            confirmButton = {
                TextButton(
                    onClick = actions.onConfirmVideoPurge,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.editor_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissVideoPurge) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}
