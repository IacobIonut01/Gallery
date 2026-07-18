/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.dateformat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dot.gallery.R

private data class FormatRow(
    val pattern: String,
    val description: String,
    val example: String,
)

private val timeRows = listOf(
    FormatRow("H:mm", "24-hour (0–23)", "9:50"),
    FormatRow("HH:mm", "24-hour (00–23)", "09:50"),
    FormatRow("h:mm a", "12-hour + marker", "1:00 PM"),
    FormatRow("hh:mm a", "12-hour (01–12) + marker", "01:00 PM"),
    FormatRow("mm", "Minutes (2 digits)", "05"),
    FormatRow("ss", "Seconds (2 digits)", "09"),
    FormatRow("a", "AM/PM marker", "PM"),
)

private val dateRows = listOf(
    FormatRow("yyyy", "Year (4 digits)", "2026"),
    FormatRow("yy", "Year (2 digits)", "26"),
    FormatRow("MMMM", "Month (full name)", "June"),
    FormatRow("MMM", "Month (abbreviated)", "Jun"),
    FormatRow("MM", "Month (2 digits)", "06"),
    FormatRow("M", "Month (no padding)", "6"),
    FormatRow("dd", "Day of month (2 digits)", "05"),
    FormatRow("d", "Day of month (no padding)", "5"),
    FormatRow("EEEE", "Weekday (full)", "Monday"),
    FormatRow("EEE", "Weekday (abbreviated)", "Mon"),
    FormatRow("D", "Day of year", "156"),
    FormatRow("w", "Week of year", "23"),
    FormatRow("G", "Era designator", "AD"),
)

private val standardRows = listOf(
    FormatRow("yyyy-MM-dd", "ISO 8601 date", "2026-06-15"),
    FormatRow("dd/MM/yyyy", "European date", "15/06/2026"),
    FormatRow("MM/dd/yyyy", "US date", "06/15/2026"),
    FormatRow("MMMM d, yyyy", "Long text date", "June 15, 2026"),
    FormatRow("EEE, MMM d", "Abbreviated date", "Mon, Jun 15"),
    FormatRow("EEEE, MMMM d, yyyy", "Full text date", "Monday, June 15, 2026"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFormatInfoSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.date_format_info_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                Text(
                    text = stringResource(R.string.date_format_info_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            sectionHeader(R.string.date_format_info_time_section)
            formatRows(timeRows)

            item { Spacer(Modifier.height(16.dp)) }
            sectionHeader(R.string.date_format_info_date_section)
            formatRows(dateRows)

            item { Spacer(Modifier.height(16.dp)) }
            sectionHeader(R.string.date_format_info_standard_section)
            formatRows(standardRows)

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sectionHeader(resId: Int) {
    item {
        Text(
            text = stringResource(resId),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.formatRows(rows: List<FormatRow>) {
    itemsIndexed(rows) { index, row ->
        val shape = when {
            rows.size == 1 -> RoundedCornerShape(16.dp)
            index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            index == rows.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            else -> RoundedCornerShape(4.dp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = row.pattern,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = row.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.4f)
            )
            Text(
                text = row.example,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
