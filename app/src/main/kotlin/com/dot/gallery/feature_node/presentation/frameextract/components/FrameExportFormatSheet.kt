package com.dot.gallery.feature_node.presentation.frameextract.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.frameextract.FrameExportFormat

@Composable
fun FrameExportFormatSheet(
    selected: FrameExportFormat,
    onSelect: (FrameExportFormat) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        title = { Text(stringResource(R.string.frame_picker_format_title)) },
        text = {
            Column {
                FormatRow(
                    title = "JPEG",
                    description = stringResource(R.string.frame_picker_format_jpeg_description),
                    selected = selected == FrameExportFormat.JPEG,
                    onClick = { onSelect(FrameExportFormat.JPEG) },
                )
                FormatRow(
                    title = "PNG",
                    description = stringResource(R.string.frame_picker_format_png_description),
                    selected = selected == FrameExportFormat.PNG,
                    onClick = { onSelect(FrameExportFormat.PNG) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.frame_picker_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun FormatRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title)
            Text(description)
        }
    }
}
