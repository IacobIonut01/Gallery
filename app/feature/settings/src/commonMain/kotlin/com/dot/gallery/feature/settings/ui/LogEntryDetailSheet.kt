/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dot.gallery.core.logging.Breadcrumb
import com.dot.gallery.core.logging.LogEntry
import com.dot.gallery.core.logging.formatLogDetailTime
import com.dot.gallery.core.logging.formatLogRowTime
import com.dot.gallery.core.logging.logLevelColor
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.ui.util.AppBottomSheetState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.settings.generated.resources.Res
import com.dot.gallery.feature.settings.generated.resources.dev_breadcrumbs_before
import com.dot.gallery.feature.settings.generated.resources.dev_context
import com.dot.gallery.feature.settings.generated.resources.dev_copy_entry
import com.dot.gallery.feature.settings.generated.resources.dev_report_error
import com.dot.gallery.feature.settings.generated.resources.dev_stacktrace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntryDetailSheet(
    state: AppBottomSheetState,
    entry: LogEntry?,
    onCopy: (LogEntry) -> Unit,
    onReport: (LogEntry) -> Unit
) {
    val scope = rememberCoroutineScope()
    if (state.isVisible && entry != null) {
        val color = logLevelColor(entry.logLevel)
        ModalBottomSheet(
            sheetState = state.sheetState,
            onDismissRequest = { scope.launch { state.hide() } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = entry.logLevel.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = entry.tag,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatLogDetailTime(entry.ts),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SelectionContainer {
                    Text(
                        text = entry.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                val location = listOfNotNull(entry.screen, entry.scope)
                    .joinToString(" · ")
                if (location.isNotEmpty()) {
                    Text(
                        text = location,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                entry.ctx?.takeIf { it.isNotEmpty() }?.let { ctx ->
                    DetailSectionTitle(stringResource(Res.string.dev_context))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ctx.forEach { (k, v) ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        text = k,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.widthIn(max = 140.dp)
                                    )
                                    SelectionContainer {
                                        Text(
                                            text = v,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                entry.stackTrace?.let { trace ->
                    DetailSectionTitle(stringResource(Res.string.dev_stacktrace))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        SelectionContainer {
                            Text(
                                text = trace,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp)
                            )
                        }
                    }
                }

                entry.crumbs?.takeIf { it.isNotEmpty() }?.let { crumbs ->
                    DetailSectionTitle(stringResource(Res.string.dev_breadcrumbs_before))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            crumbs.forEach { crumb ->
                                BreadcrumbRow(crumb)
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    FilledTonalButton(onClick = { onCopy(entry) }) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(16.dp)
                        )
                        Text(stringResource(Res.string.dev_copy_entry))
                    }
                    if (entry.isError) {
                        FilledTonalButton(onClick = { onReport(entry) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(16.dp)
                            )
                            Text(stringResource(Res.string.dev_report_error))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun BreadcrumbRow(crumb: Breadcrumb) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = formatLogRowTime(crumb.ts),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = crumb.label + crumb.ctx
                ?.takeIf { it.isNotEmpty() }
                ?.let { ctx -> "  " + ctx.entries.joinToString(" ") { "${it.key}=${it.value}" } }
                .orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 10.dp)
                .alpha(0.9f)
        )
    }
}
