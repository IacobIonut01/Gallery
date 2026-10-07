/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.logging.LogLevel
import com.dot.gallery.core.logging.logLevelColor
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.settings.generated.resources.Res
import com.dot.gallery.feature.settings.generated.resources.dev_range_1h
import com.dot.gallery.feature.settings.generated.resources.dev_range_24h
import com.dot.gallery.feature.settings.generated.resources.dev_range_7d
import com.dot.gallery.feature.settings.generated.resources.dev_range_all
import com.dot.gallery.feature.settings.generated.resources.dev_range_custom

@Composable
internal fun LogFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color? = null,
) {
    val accent = accentColor ?: MaterialTheme.colorScheme.primary
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !selected -> MaterialTheme.colorScheme.surfaceContainerLowest
            accentColor != null -> accent.copy(alpha = 0.18f)
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        label = "chipBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            if (accentColor != null) accent else MaterialTheme.colorScheme.onPrimaryContainer
        } else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        label = "chipBorder"
    )
    val shape = RoundedCornerShape(100)
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .selectable(
                selected = selected,
                role = Role.Checkbox,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .clip(shape)
                .background(backgroundColor, shape)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LogFiltersCard(
    filters: SessionLogsFilters,
    onToggleLevel: (LogLevel) -> Unit,
    onRange: (SessionLogsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LogLevel.entries.forEach { level ->
                    LogFilterChip(
                        label = level.name.lowercase()
                            .replaceFirstChar { it.titlecase() },
                        selected = level in filters.levels,
                        onClick = { onToggleLevel(level) },
                        accentColor = logLevelColor(level)
                    )
                }
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val ranges = listOf(
                    SessionLogsRange.H1 to Res.string.dev_range_1h,
                    SessionLogsRange.D1 to Res.string.dev_range_24h,
                    SessionLogsRange.D7 to Res.string.dev_range_7d,
                    SessionLogsRange.ALL to Res.string.dev_range_all,
                    SessionLogsRange.CUSTOM to Res.string.dev_range_custom,
                )
                ranges.forEach { (range, label) ->
                    LogFilterChip(
                        label = stringResource(label),
                        selected = filters.range == range,
                        onClick = { onRange(range) }
                    )
                }
            }
        }
    }
}
